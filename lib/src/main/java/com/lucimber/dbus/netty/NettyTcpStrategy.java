/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionContext;
import com.lucimber.dbus.connection.ConnectionHandle;
import com.lucimber.dbus.connection.ConnectionStrategy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty-based connection strategy for TCP transport.
 * <p>
 * This strategy handles connections over TCP/IP sockets using NIO transport
 * with optimizations for D-Bus communication.
 */
public final class NettyTcpStrategy implements ConnectionStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyTcpStrategy.class);

  @Override
  public boolean supports(SocketAddress address) {
    return address instanceof InetSocketAddress;
  }

  @Override
  public CompletionStage<ConnectionHandle> connect(SocketAddress address,
                                                   ConnectionConfig config,
                                                   ConnectionContext context) {
    EventLoopGroup workerGroup = createEventLoopGroup();
    Promise<Void> nettyConnectPromise = workerGroup.next().newPromise();
    CompletableFuture<ConnectionHandle> handleFuture = new CompletableFuture<>();

    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) config.getConnectTimeout().toMillis())
            .handler(new DBusChannelInitializer(
                    createAppLogicHandler(context, config),
                    nettyConnectPromise));

    LOGGER.info("Attempting TCP connection to {}", address);
    ChannelFuture channelFuture = bootstrap.connect(address);

    channelFuture.addListener(future -> {
      if (future.isSuccess()) {
        LOGGER.debug("TCP connection established to {}", address);
        // Create handle but wait for D-Bus handshake completion before resolving the future
        NettyConnectionHandle handle = new NettyConnectionHandle(channelFuture.channel(), workerGroup);

        // The nettyConnectPromise will be completed by ConnectionCompletionHandler
        // when MANDATORY_NAME_ACQUIRED event is received
        nettyConnectPromise.addListener(connectResult -> {
          if (connectResult.isSuccess()) {
            LOGGER.debug("D-Bus handshake completed for TCP connection");
            // Notify context that connection is fully established
            context.onConnectionEstablished();
            handleFuture.complete(handle);
          } else {
            LOGGER.error("D-Bus handshake failed for TCP connection", connectResult.cause());
            context.onError(connectResult.cause());
            handleFuture.completeExceptionally(connectResult.cause());
          }
        });
      } else {
        LOGGER.error("Failed to establish TCP connection to {}", address, future.cause());
        handleFuture.completeExceptionally(future.cause());
      }
    });

    return handleFuture;
  }

  @Override
  public String getTransportName() {
    return "NIO (TCP/IP)";
  }

  @Override
  public boolean isAvailable() {
    return true; // NIO is always available
  }

  private EventLoopGroup createEventLoopGroup() {
    LOGGER.debug("Creating NIO EventLoopGroup for TCP connection");
    return new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
  }

  private AppLogicHandler createAppLogicHandler(ConnectionContext context, ConnectionConfig config) {
    // Extract the connection from the context - this requires the context to provide access
    if (!(context instanceof NettyConnectionContext nettyContext)) {
      throw new IllegalArgumentException("TCP strategy requires NettyConnectionContext");
    }

    // Create executor service for this connection
    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            runnable -> {
              Thread t = new Thread(runnable, "dbus-app-worker-" + System.identityHashCode(runnable));
              t.setDaemon(true);
              return t;
            });

    // Get the connection from context - this will need to be added to NettyConnectionContext
    Connection connection = nettyContext.getConnection();
    return new AppLogicHandler(executor, connection, config.getMethodCallTimeoutMs());
  }
}