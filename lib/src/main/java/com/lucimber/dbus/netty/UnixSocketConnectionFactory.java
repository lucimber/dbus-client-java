/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.*;
import com.lucimber.dbus.connection.impl.DefaultPipelineFactory;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link ConnectionFactory} that creates {@link Connection}s with UNIX Domain Sockets
 * and is based on the Netty framework.
 */
public final class UnixSocketConnectionFactory implements ConnectionFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final EventLoopGroup channelEventLoopGroup;
  private final String socketPath;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param socketPath Path to DBus socket (e.g. /var/run/dbus/system_bus_socket)
   */
  public UnixSocketConnectionFactory(String socketPath) {
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
  public CompletionStage<Connection> create(PipelineInitializer initializer) throws ConnectionException {
    LoggerUtils.debug(LOGGER, () -> "Creating new domain socket connection.");
    try {
      Objects.requireNonNull(initializer);
      CompletableFuture<Connection> connectionFuture = new CompletableFuture<>();
      Bootstrap b = new Bootstrap();
      b.group(channelEventLoopGroup);
      b.channel(EpollDomainSocketChannel.class);
      PipelineFactory pipelineFactory = new DefaultPipelineFactory();
      NettyConnection connection = new NettyConnection(pipelineFactory);
      CompletableFuture<Void> dbusInitFuture = new CompletableFuture<>();
      b.handler(new SocketChannelInitializer(connection, dbusInitFuture));
      ChannelFuture channelFuture = b.connect(new DomainSocketAddress(socketPath));
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
