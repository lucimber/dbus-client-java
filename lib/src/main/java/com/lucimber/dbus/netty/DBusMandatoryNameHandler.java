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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
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
public final class DBusMandatoryNameHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DBusMandatoryNameHandler.class);

  private static final DBusString DBUS_SERVICE_NAME = DBusString.valueOf("org.freedesktop.DBus");
  private static final DBusObjectPath DBUS_OBJECT_PATH = DBusObjectPath.valueOf("/org/freedesktop/DBus");
  private static final DBusString DBUS_INTERFACE_NAME = DBusString.valueOf("org.freedesktop.DBus");
  private static final DBusString HELLO_METHOD_NAME = DBusString.valueOf("Hello");
  private static final long HELLO_TIMEOUT_SECONDS = 30; // 30 second timeout for Hello call
  private State currentState = State.IDLE;
  private DBusUInt32 helloCallSerial;
  private ScheduledFuture<?> helloTimeoutFuture;

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt == DBusChannelEvent.SASL_AUTH_COMPLETE && currentState == State.IDLE) {
      LOGGER.info("[DBusMandatoryNameHandler] SASL authentication complete, sending Hello method call");

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

      ctx.writeAndFlush(helloCall).addListener(new WriteOperationListener<>(LOGGER, future -> {
        if (future.isSuccess()) {
          LOGGER.info("[DBusMandatoryNameHandler] Hello call sent successfully (serial={})",
                  helloCallSerial.getDelegate());
          currentState = State.AWAITING_HELLO_REPLY;
          startHelloTimeout(ctx);
        } else {
          LOGGER.error("[DBusMandatoryNameHandler] Failed to send Hello call (serial={}): {}",
                  helloCallSerial.getDelegate(), future.cause().getMessage());
          ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
          ctx.pipeline().remove(this); // Remove self on send failure
          ctx.close(); // Critical failure
        }
      }));
    } else {
      // Pass on other user events if not handled here
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (currentState != State.AWAITING_HELLO_REPLY) {
      ctx.fireChannelRead(msg);
      return;
    }

    boolean handled = false;
    if (msg instanceof InboundMethodReturn methodReturn) {
      if (methodReturn.getReplySerial().equals(helloCallSerial)) {
        handled = true;
        LOGGER.info("[DBusMandatoryNameHandler] Received Hello reply (serial={})",
                methodReturn.getSerial().getDelegate());
        handleHelloReply(ctx, methodReturn);
        ReferenceCountUtil.release(msg); // Release message after handling
      }
    } else if (msg instanceof InboundError error) {
      if (error.getReplySerial().equals(helloCallSerial)) {
        handled = true;
        LOGGER.error("[DBusMandatoryNameHandler] Received Hello error (serial={}): {}",
                error.getSerial().getDelegate(), error.getErrorName());
        handleHelloError(ctx, error);
        ReferenceCountUtil.release(msg); // Release message after handling
      }
    }

    if (!handled) {
      // Not a reply to our Hello call, pass it on.
      ctx.fireChannelRead(msg);
    }
  }

  private void handleHelloReply(ChannelHandlerContext ctx, InboundMethodReturn reply) {
    cancelHelloTimeout();

    List<DBusType> payload = reply.getPayload();

    if (!payload.isEmpty() && payload.get(0) instanceof DBusString assignedName) {
      LOGGER.info("[DBusMandatoryNameHandler] Successfully acquired bus name: {}", assignedName);
      ctx.channel().attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).set(assignedName);

      // Remove handler first, then propagate event so all handlers can react
      try {
        ctx.pipeline().remove(this);
        LOGGER.debug("Removed myself as {} from pipeline.", this.getClass().getSimpleName());
      } catch (NoSuchElementException ignored) {
        LOGGER.warn("Failed to remove myself as {} from pipeline.", this.getClass().getSimpleName());
      }

      ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUIRED);
    } else {
      LOGGER.error("[DBusMandatoryNameHandler] Invalid Hello reply payload: {}", payload);

      // Remove handler first, then propagate event so all handlers can react
      try {
        ctx.pipeline().remove(this);
        LOGGER.debug("Removed myself as {} from pipeline.", this.getClass().getSimpleName());
      } catch (NoSuchElementException ignored) {
        LOGGER.warn("Failed to remove myself as {} from pipeline.", this.getClass().getSimpleName());
      }

      ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
    }
  }

  private void handleHelloError(ChannelHandlerContext ctx, InboundError error) {
    cancelHelloTimeout();

    LOGGER.error("Received error reply for Hello call (serial {}): Name: {}, Message: {}",
            helloCallSerial.getDelegate(), error.getErrorName(), error.getPayload());

    // Remove handler first, then propagate event so all handlers can react
    try {
      ctx.pipeline().remove(this);
      LOGGER.debug("Removed myself as {} from pipeline after error.", this.getClass().getSimpleName());
    } catch (NoSuchElementException ignored) {
      LOGGER.warn("Failed to remove myself as {} from pipeline after error.", this.getClass().getSimpleName());
    }

    ctx.fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.error("Exception caught. Current state: {}. Closing channel.", currentState, cause);
    // Ensure we signal failure if we were awaiting reply
    if (currentState == State.AWAITING_HELLO_REPLY) {
      cancelHelloTimeout();

      // Remove handler first, then propagate event so all handlers can react
      try {
        ctx.pipeline().remove(this);
        LOGGER.debug("Removed myself as {} from pipeline after exception.", this.getClass().getSimpleName());
      } catch (NoSuchElementException ignored) {
        LOGGER.warn("Failed to remove myself as {} from pipeline after exception.", this.getClass().getSimpleName());
      }

      ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.warn("Channel became inactive while in MandatoryNameHandler. State: {}", currentState);
    if (currentState == State.AWAITING_HELLO_REPLY) {
      cancelHelloTimeout();
      ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
    }
    // No need to remove self, pipeline is being torn down
    super.channelInactive(ctx);
  }

  private void startHelloTimeout(ChannelHandlerContext ctx) {
    helloTimeoutFuture = ctx.executor().schedule(() -> {
      if (currentState == State.AWAITING_HELLO_REPLY) {
        LOGGER.error("[DBusMandatoryNameHandler] Hello call timed out after {} seconds", HELLO_TIMEOUT_SECONDS);

        // Remove handler first, then propagate event so all handlers can react
        try {
          ctx.pipeline().remove(this);
          LOGGER.debug("Removed myself as {} from pipeline after timeout.", this.getClass().getSimpleName());
        } catch (NoSuchElementException ignored) {
          LOGGER.warn("Failed to remove myself as {} from pipeline after timeout.", this.getClass().getSimpleName());
        }

        ctx.pipeline().fireUserEventTriggered(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
      }
    }, HELLO_TIMEOUT_SECONDS, TimeUnit.SECONDS);
  }

  private void cancelHelloTimeout() {
    if (helloTimeoutFuture != null && !helloTimeoutFuture.isDone()) {
      helloTimeoutFuture.cancel(false);
    }
  }

  private enum State {
    IDLE,
    AWAITING_HELLO_REPLY
  }
}
