/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An outbound handler that encodes SASL messages to strings.
 *
 * @see SaslMessage
 */
@ChannelHandler.Sharable
final class SaslMessageEncoder extends MessageToMessageEncoder<SaslMessage> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_SASL_OUTBOUND);

  @Override
  protected void encode(final ChannelHandlerContext ctx, final SaslMessage msg, final List<Object> out) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding SASL message to string: " + msg.toString());
    final StringBuilder sb = new StringBuilder();
    sb.append(msg.getCommandName().toString());
    if (msg.getCommandValue().isPresent()) {
      sb.append(' ');
      sb.append(msg.getCommandValue().get());
    }
    out.add(sb.toString());
  }
}
