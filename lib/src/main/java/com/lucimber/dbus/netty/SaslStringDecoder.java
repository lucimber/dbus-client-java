/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.sasl.SaslAgreeMessage;
import com.lucimber.dbus.connection.sasl.SaslAuthMessage;
import com.lucimber.dbus.connection.sasl.SaslBeginMessage;
import com.lucimber.dbus.connection.sasl.SaslCancelMessage;
import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslDataMessage;
import com.lucimber.dbus.connection.sasl.SaslErrorMessage;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.connection.sasl.SaslNegotiateMessage;
import com.lucimber.dbus.connection.sasl.SaslOkMessage;
import com.lucimber.dbus.connection.sasl.SaslRejectedMessage;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An inbound handler that decodes strings to SASL messages.
 *
 * @see SaslMessage
 */
final class SaslStringDecoder extends MessageToMessageDecoder<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_SASL_INBOUND);

  private static boolean isUpperCase(final String s) {
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isUpperCase(s.charAt(i))) {
        LoggerUtils.trace(LOGGER, () -> "Command name is not uppercase.");
        return false;
      }
    }
    LoggerUtils.trace(LOGGER, () -> "Command name is uppercase.");
    return true;
  }

  private static SaslMessage map(final SaslCommandName cmdName, final String cmdValue) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping SASL command to message.");
    switch (cmdName) {
      case CLIENT_AUTH:
        return new SaslAuthMessage(cmdValue);
      case CLIENT_BEGIN:
        return new SaslBeginMessage();
      case CLIENT_CANCEL:
        return new SaslCancelMessage();
      case CLIENT_NEGOTIATE:
        return new SaslNegotiateMessage();
      case SERVER_AGREE:
        return new SaslAgreeMessage();
      case SERVER_OK:
        return new SaslOkMessage(cmdValue);
      case SERVER_REJECTED:
        return new SaslRejectedMessage(cmdValue);
      case SHARED_DATA:
        return new SaslDataMessage(cmdValue);
      case SHARED_ERROR:
        return new SaslErrorMessage(cmdValue);
      default:
        throw new UnsupportedMessageTypeException("unknown command name");
    }
  }

  @Override
  public void decode(final ChannelHandlerContext ctx, final String msg, final List<Object> out)
          throws DecoderException {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Decoding string to SASL message.");
    final int spaceIdx = msg.indexOf(' ');
    final String cmdName = spaceIdx == -1 ? msg : msg.substring(0, spaceIdx);
    final boolean isUpperCase = isUpperCase(cmdName);
    if (!isUpperCase) {
      throw new DecoderException("command name must be uppercase");
    }
    final SaslCommandName cmdNameEnum = SaslCommandName.fromString(cmdName);
    if (cmdNameEnum == null) {
      throw new DecoderException("unknown command name");
    }
    final String cmdValue = spaceIdx < 0 ? null : msg.substring(spaceIdx).trim();
    final SaslMessage message = map(cmdNameEnum, cmdValue);
    LoggerUtils.debug(LOGGER, () -> "Decoded string to " + message);
    out.add(message);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    if (cause instanceof DecoderException) {
      LoggerUtils.debug(LOGGER, MARKER,
              () -> "Handling caught exception by responding with SASL error message.");
      final String error = cause.getMessage() != null && !cause.getMessage().isEmpty()
              ? cause.getMessage() : "Experienced a frame exception";
      final SaslMessage msg = new SaslErrorMessage(error);
      final ChannelFuture future = ctx.writeAndFlush(msg);
      future.addListener(v -> {
        if (v.isDone() && v.isSuccess()) {
          LoggerUtils.trace(LOGGER, MARKER, () -> "I/O operation was completed successfully.");
        } else if (v.isDone() && v.cause() != null) {
          LoggerUtils.error(LOGGER, MARKER, () -> "I/O operation was completed with failure.", v.cause());
        } else if (v.isDone() && v.isCancelled()) {
          LoggerUtils.trace(LOGGER, MARKER, () -> "I/O operation was completed by cancellation.");
        }
      });
    } else {
      LoggerUtils.debug(LOGGER, () -> "Ignoring and passing caught exception to next inbound handler.");
      LoggerUtils.trace(LOGGER, cause::toString);
      ctx.fireExceptionCaught(cause);
    }
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Received new user event: " + evt);
    if (evt == CustomChannelEvent.SASL_AUTH_COMPLETE) {
      LoggerUtils.debug(LOGGER, MARKER, () -> "Detaching from channel pipeline.");
      ctx.pipeline().remove(this);
    }
    LoggerUtils.trace(LOGGER, () -> "Handing over user event to next handler in pipeline.");
    ctx.fireUserEventTriggered(evt);
  }
}
