/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * After SASL authentication completes and the DBus message pipeline is configured,
 * this handler sends the mandatory org.freedesktop.DBus.Hello method call
 * to request a unique bus name.
 *
 * <p>It listens for the reply to this specific Hello call. On success, it stores
 * the assigned bus name as a channel attribute and fires a
 * {@link DBusChannelEvent#MANDATORY_NAME_ACQUIRED} event. On failure, it fires
 * {@link DBusChannelEvent#MANDATORY_NAME_ACQUISITION_FAILED}.
 *
 * <p>Regardless of success or failure of the Hello call, this handler removes
 * itself from the pipeline after processing the reply or an error related to it.
 */
public final class DBusMandatoryNameHandler extends SimpleChannelInboundHandler<Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DBusMandatoryNameHandler.class);

  private static final DBusString DBUS_SERVICE_NAME = DBusString.valueOf("org.freedesktop.DBus");
  private static final DBusObjectPath DBUS_OBJECT_PATH = DBusObjectPath.valueOf("/org/freedesktop/DBus");
  private static final DBusString DBUS_INTERFACE_NAME = DBusString.valueOf("org.freedesktop.DBus");
  private static final DBusString HELLO_METHOD_NAME = DBusString.valueOf("Hello");
  private State currentState = State.IDLE;
  private DBusUInt32 helloCallSerial;

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    LOGGER.debug("Received user event: {}, current state: {}", evt, currentState);
    if (evt == DBusChannelEvent.SASL_AUTH_COMPLETE && currentState == State.IDLE) {
      LOGGER.debug("DBus message pipeline is ready. Sending org.freedesktop.DBus.Hello method call.");

      AtomicLong serialCounter = ctx.channel().attr(DBusChannelAttribute.SERIAL_COUNTER).get();
      // D-Bus serial numbers are 32-bit unsigned and allowed to wrap around
      helloCallSerial = DBusUInt32.valueOf((int) serialCounter.getAndIncrement());

      OutboundMethodCall helloCall = OutboundMethodCall.Builder
              .create()
              .withSerial(helloCallSerial)
              .withPath(DBUS_OBJECT_PATH)
              .withMember(HELLO_METHOD_NAME)
              .withReplyExpected(true)
              .withDestination(DBUS_SERVICE_NAME)
              .withInterface(DBUS_INTERFACE_NAME)
              .build();

      ctx.writeAndFlush(helloCall).addListener(future -> {
        if (future.isSuccess()) {
          LOGGER.debug("Hello call (serial {}) sent successfully.", helloCallSerial.getDelegate());
          currentState = State.AWAITING_HELLO_REPLY;
        } else {
          LOGGER.error("Failed to send Hello call (serial {}).", helloCallSerial.getDelegate(), future.cause());
          ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
          ctx.pipeline().remove(this); // Remove self on send failure
          ctx.close(); // Critical failure
        }
      });
    } else {
      // Pass on other user events if not handled here
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (currentState != State.AWAITING_HELLO_REPLY) {
      // Not waiting for our Hello reply, pass it on.
      ctx.fireChannelRead(msg);
      return;
    }

    boolean handled = false;
    if (msg instanceof InboundMethodReturn methodReturn) {
      if (methodReturn.getReplySerial().equals(helloCallSerial)) {
        handled = true;
        handleHelloReply(ctx, methodReturn);
      }
    } else if (msg instanceof InboundError error) {
      if (error.getReplySerial().equals(helloCallSerial)) {
        handled = true;
        handleHelloError(ctx, error);
      }
    }

    if (!handled) {
      // Not a reply to our Hello call, pass it on.
      ctx.fireChannelRead(msg);
    }
  }

  private void handleHelloReply(ChannelHandlerContext ctx, InboundMethodReturn reply) {
    LOGGER.debug("Received reply for Hello call (serial {}): {}", helloCallSerial.getDelegate(), reply);
    List<DBusType> payload = reply.getPayload();

    if (!payload.isEmpty() && payload.get(0) instanceof DBusString assignedName) {
      LOGGER.info("Successfully acquired bus name: {}", assignedName);
      ctx.channel().attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).set(assignedName);
      ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUIRED);
    } else {
      LOGGER.error("Hello reply did not contain a valid string name in payload. Payload: {}", payload);
      ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
    }

    try {
      ctx.pipeline().remove(this);
      LOGGER.debug("Removed myself as {} from pipeline.", this.getClass().getSimpleName());
    } catch (NoSuchElementException ignored) {
      LOGGER.warn("Failed to remove myself as {} from pipeline.", this.getClass().getSimpleName());
    }
  }

  private void handleHelloError(ChannelHandlerContext ctx, InboundError error) {
    LOGGER.error("Received error reply for Hello call (serial {}): Name: {}, Message: {}",
            helloCallSerial.getDelegate(), error.getErrorName(), error.getPayload());
    ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.error("Exception caught. Current state: {}. Closing channel.", currentState, cause);
    // Ensure we signal failure if we were awaiting reply
    if (currentState == State.AWAITING_HELLO_REPLY) {
      ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.warn("Channel became inactive while in MandatoryNameHandler. State: {}", currentState);
    if (currentState == State.AWAITING_HELLO_REPLY) {
      ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
    }
    // No need to remove self, pipeline is being torn down
    super.channelInactive(ctx);
  }

  private enum State {
    IDLE,
    AWAITING_HELLO_REPLY
  }
}
