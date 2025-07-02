/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.DefaultPipeline;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.UInt32;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class NettyConnection implements Connection {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnection.class);

  private final EventLoopGroup workerGroup;
  private final ExecutorService applicationTaskExecutor;
  private Channel channel;
  private AppLogicHandler appLogicHandler;

  private final SocketAddress serverAddress;
  private final Class<? extends Channel> channelClass;

  private final Pipeline pipeline;

  public NettyConnection(SocketAddress serverAddress) {
    this.serverAddress = Objects.requireNonNull(serverAddress, "serverAddress must not be NULL");
    if (serverAddress instanceof DomainSocketAddress) {
      if (Epoll.isAvailable()) {
        this.workerGroup = new MultiThreadIoEventLoopGroup(1, EpollIoHandler.newFactory());
        this.channelClass = EpollDomainSocketChannel.class;
        LOGGER.info("Using Epoll transport for Unix Domain Socket.");
      } else if (KQueue.isAvailable()) {
        this.workerGroup = new MultiThreadIoEventLoopGroup(1, KQueueIoHandler.newFactory());
        this.channelClass = KQueueDomainSocketChannel.class;
        LOGGER.info("Using KQueue transport for Unix Domain Socket.");
      } else {
        throw new UnsupportedOperationException("Unix Domain Sockets require Epoll (Linux)" +
              " or KQueue (macOS) native transport, but neither is available.");
      }
    } else if (serverAddress instanceof InetSocketAddress) {
      this.workerGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
      this.channelClass = NioSocketChannel.class;
      LOGGER.info("Using NIO transport for TCP/IP socket.");
    } else {
      throw new IllegalArgumentException("Unsupported server address type: " + serverAddress.getClass());
    }

    this.pipeline = new DefaultPipeline(this);

    this.applicationTaskExecutor = Executors.newFixedThreadPool(
          Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
          runnable -> {
            Thread t = new Thread(runnable, "dbus-app-worker-" + System.identityHashCode(runnable));
            t.setDaemon(true);
            return t;
          });
  }

  /**
   * Creates a connection for the standard system bus path.
   * (Typically /var/run/dbus/system_bus_socket)
   *
   * @return A new instance.
   * @throws UnsupportedOperationException if native transport for UDS is not available.
   */
  public static NettyConnection newSystemBusConnection() {
    // Standard system bus path, can be overridden by DBUS_SYSTEM_BUS_ADDRESS env var
    String path = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
    if (path == null || path.isEmpty()) {
      path = "/var/run/dbus/system_bus_socket"; // Default
    } else if (path.startsWith("unix:path=")) {
      path = path.substring("unix:path=".length());
    } else {
      // Handle other address formats if necessary (e.g., abstract sockets, tcp)
      LOGGER.warn("DBUS_SYSTEM_BUS_ADDRESS format not fully parsed, using raw value: {}", path);
    }
    return new NettyConnection(new DomainSocketAddress(path));
  }

  /**
   * Creates a connection for the standard session bus path.
   * (Path is usually obtained from DBUS_SESSION_BUS_ADDRESS env var)
   *
   * @return A new instance.
   * @throws UnsupportedOperationException if native transport for UDS is not available or address not found.
   */
  public static NettyConnection newSessionBusConnection() {
    String address = System.getenv("DBUS_SESSION_BUS_ADDRESS");
    if (address == null || address.isEmpty()) {
      throw new IllegalStateException("DBUS_SESSION_BUS_ADDRESS environment variable not set.");
    }
    // DBUS_SESSION_BUS_ADDRESS can be complex, e.g., "unix:path=/tmp/dbus-...,guid=..."
    // For now, we'll parse simple "unix:path="
    if (address.startsWith("unix:path=")) {
      String path = address.substring("unix:path=".length());
      // It might have a comma and other params, e.g., unix:path=/tmp/dbus-...,guid=...
      int commaIndex = path.indexOf(',');
      if (commaIndex != -1) {
        path = path.substring(0, commaIndex);
      }
      return new NettyConnection(new DomainSocketAddress(path));
    } else if (address.startsWith("tcp:host=")) {
      // Example: tcp:host=localhost,port=12345 or tcp:host=127.0.0.1,port=12345,family=ipv4
      try {
        String host = null;
        int port = -1;
        String[] params = address.substring("tcp:".length()).split(",");
        for (String param : params) {
          String[] kv = param.split("=");
          if (kv.length == 2) {
            if ("host".equals(kv[0])) host = kv[1];
            else if ("port".equals(kv[0])) port = Integer.parseInt(kv[1]);
          }
        }
        if (host != null && port != -1) {
          return new NettyConnection(new InetSocketAddress(host, port));
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("Could not parse TCP DBUS_SESSION_BUS_ADDRESS: " + address, e);
      }
    }
    throw new IllegalArgumentException("Unsupported DBUS_SESSION_BUS_ADDRESS format: " + address +
          ". Only simple 'unix:path=' or 'tcp:host=...,port=...' currently supported.");
  }

  @Override
  public CompletionStage<Void> connect() {
    // Ensure appLogicHandler is fresh for each connect attempt if connection can be retried.
    // For now, assuming one connect call per Connection instance.
    if (this.appLogicHandler != null && this.channel != null && this.channel.isActive()) {
      LOGGER.warn("Already connected or connection attempt in progress.");
      Promise<Void> alreadyConnectedPromise = workerGroup.next().newPromise();
      alreadyConnectedPromise.setSuccess(null); // Or an error if preferred
      return NettyFutureConverter.toCompletionStage(alreadyConnectedPromise);
    }
    this.appLogicHandler = new AppLogicHandler(applicationTaskExecutor, this);

    Promise<Void> connectPromise = workerGroup.next().newPromise();

    Bootstrap b = new Bootstrap();
    b.group(workerGroup).channel(this.channelClass); // Use determined channel class

    if (this.serverAddress instanceof InetSocketAddress) { // Only set for TCP
      b.option(ChannelOption.SO_KEEPALIVE, true);
    }

    b.handler(new DBusChannelInitializer(appLogicHandler, connectPromise));

    LOGGER.info("Attempting to connect to DBus server at {}", serverAddress);
    ChannelFuture channelFuture = b.connect(serverAddress);

    channelFuture.addListener((ChannelFuture cf) -> {
      if (cf.isSuccess()) {
        LOGGER.debug("Socket connection established to {}.", serverAddress);
        this.channel = cf.channel();
      } else {
        LOGGER.error("Failed to establish socket connection to {}.", serverAddress, cf.cause());
        connectPromise.tryFailure(cf.cause());
      }
    });

    return NettyFutureConverter.toCompletionStage(connectPromise);
  }

  @Override
  public boolean isConnected() {
    return channel != null && channel.isActive() &&
          channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get() != null;
  }

  @Override
  public void close() {
    LOGGER.info("Closing DBus connection to {}...", serverAddress);
    if (channel != null) {
      channel.close().awaitUninterruptibly(5, TimeUnit.SECONDS);
    }
    if (applicationTaskExecutor != null && !applicationTaskExecutor.isShutdown()) {
      applicationTaskExecutor.shutdown();
      try {
        if (!applicationTaskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          applicationTaskExecutor.shutdownNow();
        }
      } catch (InterruptedException ie) {
        applicationTaskExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
    if (workerGroup != null && !workerGroup.isShuttingDown()) {
      // shutdownGracefully returns a Future, await it.
      Future<?> shutdownFuture = workerGroup.shutdownGracefully(1, 5, TimeUnit.SECONDS);
      // Wait a bit longer for actual shutdown
      shutdownFuture.awaitUninterruptibly(5, TimeUnit.SECONDS);
    }
    LOGGER.info("DBus connection to {} closed.", serverAddress);
  }

  @Override
  public UInt32 getNextSerial() {
    if (channel == null || !channel.isActive()) {
      throw new IllegalStateException("Cannot get next serial, channel is not active.");
    }
    AtomicLong serialCounter = channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    if (serialCounter == null) {
      throw new IllegalStateException("Serial counter not initialized on channel.");
    }
    // D-Bus serial numbers are 32-bit unsigned and allowed to wrap around
    return UInt32.valueOf((int) serialCounter.getAndIncrement());
  }

  @Override
  public Pipeline getPipeline() {
    return pipeline;
  }

  @Override
  public CompletionStage<InboundMessage> sendRequest(OutboundMessage msg) {
    if (appLogicHandler == null || !isConnected()) {
      Promise<InboundMessage> failedPromise = workerGroup.next().newPromise();
      var re = new IllegalStateException("Not connected to D-Bus.");
      failedPromise.setFailure(re);
      return NettyFutureConverter.toCompletionStage(failedPromise);
    } else {
      CompletableFuture<InboundMessage> returnFuture = new CompletableFuture<>();

      appLogicHandler.writeMessage(msg).addListener(outerFuture -> {
        if (!outerFuture.isSuccess()) {
          returnFuture.completeExceptionally(outerFuture.cause());
          return;
        }

        @SuppressWarnings("unchecked")
        Future<InboundMessage> innerFuture = (Future<InboundMessage>) outerFuture.getNow();
        innerFuture.addListener(inner -> {
          if (inner.isSuccess()) {
            returnFuture.complete((InboundMessage) inner.getNow());
          } else {
            returnFuture.completeExceptionally(inner.cause());
          }
        });
      });

      return returnFuture;
    }
  }

  @Override
  public void sendAndRouteResponse(OutboundMessage msg, CompletableFuture<Void> future) {
    if (appLogicHandler == null || !isConnected()) {
      var re = new IllegalStateException("Not connected to D-Bus.");
      future.completeExceptionally(re);
    } else {
      appLogicHandler.writeAndRouteResponse(msg).addListener(f -> {
        if (f.isSuccess()) {
          future.complete(null);
        } else if (f.cause() != null) {
          future.completeExceptionally(f.cause());
        } else if (f.isCancelled()) {
          future.cancel(true);
        }
      });
    }
  }
}
