/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.impl;

import com.lucimber.dbus.connection.Handler;
import com.lucimber.dbus.connection.HandlerContext;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A pipeline handler which implements the {@code org.freedesktop.DBus.Peer} interface.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces">
 * D-Bus Specification (Standard Interfaces)</a>
 */
public final class DbusPeerHandler implements Handler {

  private static final String DEBUG_MSG_HANDLING = "Handling %s.";
  private static final String DEBUG_MSG_PASSING = "Passing on %s to next inbound handler.";
  private static final DBusString INTERFACE = DBusString.valueOf("org.freedesktop.DBus.Peer");
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final UUID machineId;

  /**
   * Constructs a new instance of this pipeline handler.
   *
   * @param machineId A hex-encoded UUID representing the identity of the machine the process is running on.
   */
  public DbusPeerHandler(UUID machineId) {
    this.machineId = Objects.requireNonNull(machineId);
  }

  private static void respondToPing(HandlerContext ctx, InboundMethodCall methodCall) {
    DBusString dst = methodCall.getSender();
    UInt32 serial = ctx.getPipeline().getConnection().getNextSerial();
    UInt32 replySerial = methodCall.getSerial();
    OutboundMessage methodReturn = new OutboundMethodReturn(serial, replySerial, dst, null, null);
    LoggerUtils.trace(LOGGER, methodReturn::toString);
    ctx.passOutboundMessage(methodReturn);
  }

  private static void passInboundMessage(HandlerContext ctx, InboundMessage msg) {
    LoggerUtils.debug(LOGGER, () -> String.format(DEBUG_MSG_PASSING, msg));
    ctx.passInboundMessage(msg);
  }

  private void handleInboundMethodCall(HandlerContext ctx, InboundMethodCall methodCall) {
    if (methodCall.getInterfaceName().orElse(DBusString.valueOf("")).equals(INTERFACE)) {
      if (methodCall.getMember().equals(DBusString.valueOf("Ping"))) {
        LoggerUtils.debug(LOGGER, () -> String.format(DEBUG_MSG_HANDLING, methodCall));
        respondToPing(ctx, methodCall);
      } else if (methodCall.getMember().equals(DBusString.valueOf("GetMachineId"))) {
        respondWithMachineId(ctx, methodCall);
      } else {
        passInboundMessage(ctx, methodCall);
      }
    } else {
      passInboundMessage(ctx, methodCall);
    }
  }

  private void respondWithMachineId(HandlerContext ctx, InboundMethodCall methodCall) {
    UInt32 serial = ctx.getPipeline().getConnection().getNextSerial();
    UInt32 replySerial = methodCall.getSerial();
    DBusString dst = methodCall.getSender();
    Signature sig = Signature.valueOf("s");
    List<DBusType> payload = new ArrayList<>();
    payload.add(DBusString.valueOf(machineId.toString()));
    OutboundMessage methodReturn = new OutboundMethodReturn(serial, replySerial, dst, sig, payload);
    LoggerUtils.trace(LOGGER, methodReturn::toString);
    ctx.passOutboundMessage(methodReturn);
  }

  @Override
  public void onInboundMessage(HandlerContext ctx, InboundMessage msg) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Objects.requireNonNull(msg, "msg must not be null");
    if (msg instanceof InboundMethodCall) {
      handleInboundMethodCall(ctx, (InboundMethodCall) msg);
    } else {
      passInboundMessage(ctx, msg);
    }
  }
}
