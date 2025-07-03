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
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalTailHandler extends AbstractDuplexHandler implements InboundHandler, OutboundHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalTailHandler.class);
  private static final DBusString NOT_HANDLED_ERROR = DBusString.valueOf("org.freedesktop.DBus.Error.Failed");

  @Override
  Logger getLogger() {
    return LOGGER;
  }

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

  private void sendErrorReply(Context ctx, InboundMethodCall call) {
    LOGGER.debug("Sending error reply for unhandled method call: {}", call);

    Signature signature = Signature.valueOf("s");
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
