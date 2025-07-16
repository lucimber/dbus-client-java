/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.ConnectionHandle;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty-based implementation of ConnectionHandle.
 * <p>
 * This class wraps a Netty Channel and provides the transport-agnostic
 * ConnectionHandle interface for the D-Bus connection layer.
 */
public final class NettyConnectionHandle implements ConnectionHandle {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnectionHandle.class);

  private final Channel channel;
  private final EventLoopGroup eventLoopGroup;

  public NettyConnectionHandle(Channel channel, EventLoopGroup eventLoopGroup) {
    this.channel = channel;
    this.eventLoopGroup = eventLoopGroup;
  }

  @Override
  public boolean isActive() {
    return channel != null
            && channel.isActive()
            && channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get() != null;
  }

  @Override
  public CompletionStage<Void> send(OutboundMessage message) {
    if (!isActive()) {
      CompletableFuture<Void> future = new CompletableFuture<>();
      future.completeExceptionally(new IllegalStateException("Connection is not active"));
      return future;
    }

    CompletableFuture<Void> future = new CompletableFuture<>();
    channel.writeAndFlush(message).addListener(channelFuture -> {
      if (channelFuture.isSuccess()) {
        future.complete(null);
      } else {
        future.completeExceptionally(channelFuture.cause());
      }
    });
    return future;
  }

  @Override
  public CompletionStage<InboundMessage> sendRequest(OutboundMessage message) {
    if (!isActive()) {
      CompletableFuture<InboundMessage> future = new CompletableFuture<>();
      future.completeExceptionally(new IllegalStateException("Connection is not active"));
      return future;
    }

    // TODO: Implement request-response correlation
    // This would need to work with AppLogicHandler to correlate requests and responses
    CompletableFuture<InboundMessage> future = new CompletableFuture<>();
    future.completeExceptionally(new UnsupportedOperationException("Request-response not yet implemented"));
    return future;
  }

  @Override
  public DBusUInt32 getNextSerial() {
    if (!isActive()) {
      throw new IllegalStateException("Cannot get next serial, connection is not active");
    }

    AtomicLong serialCounter = channel.attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    if (serialCounter == null) {
      throw new IllegalStateException("Serial counter not initialized on channel");
    }

    return DBusUInt32.valueOf((int) serialCounter.getAndIncrement());
  }

  @Override
  public CompletionStage<Void> close() {
    LOGGER.info("Closing Netty connection handle");

    CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    if (channel != null) {
      channel.close().addListener(channelFuture -> {
        if (eventLoopGroup != null && !eventLoopGroup.isShuttingDown()) {
          Future<?> shutdownFuture = eventLoopGroup.shutdownGracefully(1, 5, TimeUnit.SECONDS);
          shutdownFuture.addListener(groupFuture -> {
            if (groupFuture.isSuccess()) {
              closeFuture.complete(null);
            } else {
              closeFuture.completeExceptionally(groupFuture.cause());
            }
          });
        } else {
          closeFuture.complete(null);
        }
      });
    } else {
      closeFuture.complete(null);
    }

    return closeFuture;
  }

  @Override
  public String getAssignedBusName() {
    if (channel == null) {
      return null;
    }
    var dbusString = channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get();
    return dbusString != null ? dbusString.toString() : null;
  }
}