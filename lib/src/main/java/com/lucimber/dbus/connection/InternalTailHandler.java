/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal tail handler that processes messages that reach the end of the pipeline.
 *
 * <p>This handler is automatically added to the tail of every pipeline and serves
 * as a final fallback for unhandled messages. It handles method calls by sending
 * appropriate error replies and logs other unhandled messages.
 *
 * <p>For unhandled method calls that expect a reply, this handler will automatically
 * send a {@code org.freedesktop.DBus.Error.Failed} error response to inform the
 * caller that no handler was able to process the request.
 *
 * @see Pipeline
 * @see Context
 * @since 1.0.0
 */
final class InternalTailHandler extends AbstractDuplexHandler implements InboundHandler, OutboundHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalTailHandler.class);
  private static final DBusString NOT_HANDLED_ERROR = DBusString.valueOf("org.freedesktop.DBus.Error.Failed");

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Handles inbound messages that reach the end of the pipeline.
   *
   * <p>This method processes different types of messages:
   * <ul>
   * <li>Method calls expecting a reply: sends an error response</li>
   * <li>Method calls not expecting a reply: logs and ignores</li>
   * <li>Method returns and errors: logs as warnings since they should have been handled</li>
   * <li>Signals and other messages: logs and ignores</li>
   * </ul>
   *
   * @param ctx the {@link Context} this handler is bound to
   * @param msg the {@link InboundMessage} being processed
   * @since 1.0.0
   */
  @Override
  public void handleInboundMessage(Context ctx, InboundMessage msg) {
    Objects.requireNonNull(msg);

    if (msg instanceof InboundMethodCall methodCall) {
      if (methodCall.isReplyExpected()) {
        sendErrorReply(ctx, methodCall);
      } else {
        LOGGER.debug("Unhandled InboundMethodCall without reply expectation: {}", methodCall);
      }
    } else if (msg instanceof InboundMethodReturn || msg instanceof InboundError) {
      LOGGER.warn("Received unhandled reply message that was not intercepted: {}", msg);
    } else {
      LOGGER.debug("Ignoring unhandled inbound signal or unknown message: {}", msg);
    }
  }

  /**
   * Sends an error reply for an unhandled method call.
   *
   * <p>This method constructs a standard D-Bus error response indicating that
   * no handler was able to process the method call.
   *
   * @param ctx  the {@link Context} this handler is bound to
   * @param call the {@link InboundMethodCall} that was not handled
   * @since 1.0.0
   */
  private void sendErrorReply(Context ctx, InboundMethodCall call) {
    LOGGER.debug("Sending error reply for unhandled method call: {}", call);

    DBusSignature signature = DBusSignature.valueOf("s");
    List<DBusType> payload = List
            .of(DBusString.valueOf("No handler was able to process the request."));

    OutboundError error = OutboundError.Builder
            .create()
            .withSerial(ctx.getConnection().getNextSerial())
            .withReplySerial(call.getSerial())
            .withErrorName(NOT_HANDLED_ERROR)
            .withDestination(call.getSender())
            .withBody(signature, payload)
            .build();

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.exceptionally(cause -> {
      LOGGER.error("Failed to send fallback error reply for serial {}",
              call.getSerial(), cause);
      return null;
    });
    ctx.propagateOutboundMessage(error, future);
  }
}
