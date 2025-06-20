/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.message.Reply;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

/**
 * Requests the mandatory service name from the bus after successful SASL authentication.
 */
final class MandatoryNameHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final UInt32 serial;

  MandatoryNameHandler(UInt32 serial) {
    this.serial = Objects.requireNonNull(serial);
  }

  private static void writeOutboundMethodCall(ChannelHandlerContext ctx, OutboundMethodCall methodCall) {
    LoggerUtils.trace(LOGGER, () -> "Writing outbound method call.");
    final ChannelFuture future = ctx.writeAndFlush(methodCall);
    final DefaultFutureListener<ChannelFuture> listener = new DefaultFutureListener<>(LOGGER);
    future.addListener(listener);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Reply) {
      handleInboundReply(ctx, (Reply) msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt == DBusChannelEvent.SASL_AUTH_COMPLETE) {
      LoggerUtils.debug(LOGGER, () -> "Requesting mandatory service name from bus.");
      final OutboundMethodCall methodCall = prepareOutboundMethodCall();
      writeOutboundMethodCall(ctx, methodCall);
    }
    ctx.fireUserEventTriggered(evt);
  }

  private void handleInboundError(InboundError error) throws Exception {
    LoggerUtils.debug(LOGGER, () -> "Handling inbound error: " + error);
    throw new Exception(error.getErrorName().toString());
  }

  private void handleInboundMethodReturn(ChannelHandlerContext ctx, InboundMethodReturn methodReturn) {
    LoggerUtils.debug(LOGGER, () -> "Handling inbound method return: " + methodReturn);
    final String warnMsg = "Could not read mandatory name from inbound method return.";
    try {
      final List<DBusType> payload = methodReturn.getPayload();
      if (payload.isEmpty()) {
        LoggerUtils.warn(LOGGER, () -> warnMsg);
      } else {
        final Object o = payload.get(0);
        final DBusString name = (DBusString) o;
        LoggerUtils.info(LOGGER, () -> "Successfully requested " + name + " as mandatory name.");
      }
    } catch (ClassCastException ex) {
      LoggerUtils.warn(LOGGER, () -> warnMsg);
    }
    ctx.pipeline().remove(this);
    ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUIRED);
  }

  private void handleInboundReply(ChannelHandlerContext ctx, Reply reply) throws Exception {
    if (reply.getReplySerial().equals(serial)) {
      if (reply instanceof InboundError) {
        handleInboundError((InboundError) reply);
      } else if (reply instanceof InboundMethodReturn) {
        handleInboundMethodReturn(ctx, (InboundMethodReturn) reply);
      } else {
        ctx.fireChannelRead(reply);
      }
    } else {
      ctx.fireChannelRead(reply);
    }
  }

  private OutboundMethodCall prepareOutboundMethodCall() {
    LoggerUtils.trace(LOGGER, () -> "Preparing outbound method call.");
    ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");
    DBusString methodName = DBusString.valueOf("Hello");
    DBusString dst = DBusString.valueOf("org.freedesktop.DBus");
    DBusString iface = DBusString.valueOf("org.freedesktop.DBus");
    return new OutboundMethodCall(serial, path, methodName, true,
          dst, iface, null, null);
  }
}
