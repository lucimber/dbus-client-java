/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.ConnectionConfig;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
  private final ConnectionConfig config;
  private final RealityCheckpoint realityCheckpoint;
  private final AtomicBoolean closing = new AtomicBoolean(false);

  public NettyConnectionHandle(Channel channel, EventLoopGroup eventLoopGroup, ConnectionConfig config, RealityCheckpoint realityCheckpoint) {
    this.channel = channel;
    this.eventLoopGroup = eventLoopGroup;
    this.config = config;
    this.realityCheckpoint = realityCheckpoint;
  }

  @Override
  public boolean isActive() {
    // Don't report as active if we're closing
    if (closing.get()) {
      return false;
    }
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

    if (realityCheckpoint == null) {
      CompletableFuture<InboundMessage> future = new CompletableFuture<>();
      future.completeExceptionally(new IllegalStateException("RealityCheckpoint not available"));
      return future;
    }

    // Use RealityCheckpoint for request-response correlation
    CompletableFuture<InboundMessage> resultFuture = new CompletableFuture<>();
    
    Future<Future<InboundMessage>> writeResult = realityCheckpoint.writeMessage(message);
    
    writeResult.addListener(writeFuture -> {
      if (writeFuture.isSuccess()) {
        // Message was successfully written, now wait for the reply
        @SuppressWarnings("unchecked")
        Future<InboundMessage> replyFuture = (Future<InboundMessage>) writeFuture.getNow();
        replyFuture.addListener(replyResult -> {
          if (replyResult.isSuccess()) {
            resultFuture.complete((InboundMessage) replyResult.getNow());
          } else {
            resultFuture.completeExceptionally(replyResult.cause());
          }
        });
      } else {
        // Failed to write the message
        resultFuture.completeExceptionally(writeFuture.cause());
      }
    });
    
    return resultFuture;
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
    // Atomic check-and-set to prevent concurrent close operations
    if (!closing.compareAndSet(false, true)) {
      LOGGER.debug("Close operation already in progress, returning completed future");
      return CompletableFuture.completedFuture(null);
    }

    LOGGER.info("Closing Netty connection handle");

    CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    if (channel != null) {
      try {
        channel.close().addListener(channelFuture -> {
          if (channelFuture.isSuccess()) {
            LOGGER.debug("Channel closed successfully");
          } else {
            LOGGER.warn("Error closing channel", channelFuture.cause());
          }
          
          if (eventLoopGroup != null && !eventLoopGroup.isShuttingDown()) {
            try {
              long quietPeriod = Math.min(1000, config.getCloseTimeout().toMillis() / 10);
              long timeout = config.getCloseTimeout().toMillis();
              Future<?> shutdownFuture = eventLoopGroup.shutdownGracefully(quietPeriod, timeout, TimeUnit.MILLISECONDS);
              shutdownFuture.addListener(groupFuture -> {
                if (groupFuture.isSuccess()) {
                  LOGGER.debug("EventLoopGroup shut down successfully");
                  closeFuture.complete(null);
                } else {
                  LOGGER.error("Error shutting down EventLoopGroup", groupFuture.cause());
                  closeFuture.completeExceptionally(groupFuture.cause());
                }
              });
            } catch (Exception e) {
              LOGGER.error("Error initiating EventLoopGroup shutdown", e);
              closeFuture.completeExceptionally(e);
            }
          } else {
            if (eventLoopGroup != null) {
              LOGGER.debug("EventLoopGroup already shutting down, skipping shutdown");
            }
            closeFuture.complete(null);
          }
        });
      } catch (Exception e) {
        LOGGER.error("Error initiating channel close", e);
        closeFuture.completeExceptionally(e);
      }
    } else {
      LOGGER.debug("Channel is null, nothing to close");
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