/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionException;
import com.lucimber.dbus.connection.ConnectionFactory;
import com.lucimber.dbus.connection.PipelineFactory;
import com.lucimber.dbus.connection.PipelineInitializer;
import com.lucimber.dbus.connection.impl.DefaultPipelineFactory;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionFactory} that creates {@link Connection}s with UNIX Domain Sockets
 * and is based on the Netty framework.
 */
public final class UnixSocketConnectionFactory implements ConnectionFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final EventLoopGroup channelEventLoopGroup;
  private final String cookiePath;
  private final String socketPath;
  private final String userId;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param userId     Numeric user ID as defined by POSIX (e.g. 1000)
   * @param cookiePath Path to DBus cookie directory (e.g. ~/.dbus-keyrings/)
   * @param socketPath Path to DBus socket (e.g. /var/run/dbus/system_bus_socket)
   */
  public UnixSocketConnectionFactory(final String userId, final String cookiePath, final String socketPath) {
    this.userId = Objects.requireNonNull(userId);
    this.cookiePath = Objects.requireNonNull(cookiePath);
    this.socketPath = Objects.requireNonNull(socketPath);
    channelEventLoopGroup = new EpollEventLoopGroup();
  }

  /**
   * Creates a connection instance asynchronously.
   *
   * @param initializer will be used to configure the connection pipeline
   * @return a completion stage with a connection
   * @throws ConnectionException If the connection instance could not be created.
   */
  public CompletionStage<Connection> create(final PipelineInitializer initializer) throws ConnectionException {
    LoggerUtils.debug(LOGGER, () -> "Creating new domain socket connection.");
    try {
      Objects.requireNonNull(initializer);
      final CompletableFuture<Connection> connectionFuture = new CompletableFuture<>();
      final Bootstrap b = new Bootstrap();
      b.group(channelEventLoopGroup);
      b.channel(EpollDomainSocketChannel.class);
      final PipelineFactory pipelineFactory = new DefaultPipelineFactory();
      final NettyConnection connection = new NettyConnection(pipelineFactory);
      final CompletableFuture<Void> dbusInitFuture = new CompletableFuture<>();
      b.handler(new SocketChannelInitializer(userId, cookiePath, connection, dbusInitFuture));
      final ChannelFuture channelFuture = b.connect(new DomainSocketAddress(socketPath));
      channelFuture.addListener(connect -> {
        if (connect.isSuccess()) {
          dbusInitFuture.whenComplete((init, t) -> {
            if (t == null) {
              initializer.initiate(connection.getPipeline());
              connectionFuture.complete(connection);
            } else {
              connectionFuture.completeExceptionally(t);
            }
          });
        } else {
          connectionFuture.completeExceptionally(connect.cause());
        }
      });
      return connectionFuture;
    } catch (Throwable t) {
      throw new ConnectionException("Could not create connection.", t);
    }
  }
}
