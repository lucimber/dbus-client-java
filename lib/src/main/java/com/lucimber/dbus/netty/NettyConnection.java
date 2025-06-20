/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.connection.PipelineFactory;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

final class NettyConnection extends ChannelDuplexHandler implements Connection {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER_INBOUND = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_INBOUND);
  private static final Marker MARKER_OUTBOUND = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_OUTBOUND);

  private final Pipeline pipeline;
  private ChannelHandlerContext ctx;

  NettyConnection(final PipelineFactory factory) {
    Objects.requireNonNull(factory, "factory must not be null");
    this.pipeline = factory.create(this);
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "I have been added to a channel context.");
    this.ctx = Objects.requireNonNull(ctx);
    AtomicLong serialCounter = ctx.channel().attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    if (serialCounter == null) {
      serialCounter = new AtomicLong(1); // Start serials at 1
      ctx.channel().attr(DBusChannelAttribute.SERIAL_COUNTER).set(serialCounter);
    }
  }

  @Override
  public void connect(final ChannelHandlerContext ctx, final SocketAddress remoteAddress,
                      final SocketAddress localAddress, final ChannelPromise promise) {
    ctx.connect(remoteAddress, localAddress, promise.addListener((ChannelFutureListener) future -> {
      if (!future.isSuccess()) {
        LoggerUtils.error(LOGGER, () -> "Could not connect to D-Bus.", future.cause());
        pipeline.passOutboundFailure(future.cause());
      }
    }));
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    LoggerUtils.debug(LOGGER, MARKER_INBOUND, () -> "Received an user event: " + evt);
    if (evt == DBusChannelEvent.SASL_AUTH_COMPLETE) {
      pipeline.passConnectionActiveEvent();
    }
    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    LoggerUtils.debug(LOGGER, MARKER_INBOUND, () -> "Received a message: " + msg);
    if (msg instanceof InboundMessage) {
      pipeline.passInboundMessage((InboundMessage) msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public Pipeline getPipeline() {
    return pipeline;
  }

  @Override
  public UInt32 getNextSerial() {
    AtomicLong serialCounter = ctx.channel().attr(DBusChannelAttribute.SERIAL_COUNTER).get();
    // D-Bus serial numbers are 32-bit unsigned and allowed to wrap around
    return UInt32.valueOf((int) serialCounter.incrementAndGet());
  }

  @Override
  public void writeOutboundMessage(final OutboundMessage msg) {
    LoggerUtils.debug(LOGGER, MARKER_OUTBOUND, () -> "Writing outbound message to channel: " + msg);
    final Consumer<ChannelFuture> consumer = future -> {
      if (future.cause() != null) {
        pipeline.passOutboundFailure(future.cause());
      }
    };
    final ChannelPromise promise = ctx.newPromise();
    promise.addListener(new DefaultFutureListener<>(LOGGER, consumer));
    ctx.writeAndFlush(msg, promise);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    LoggerUtils.debug(LOGGER, MARKER_INBOUND, () -> "Received an exception: " + cause.toString());
    pipeline.passInboundFailure(cause);
  }
}
