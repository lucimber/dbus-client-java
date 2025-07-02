/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.connection.sasl.SaslMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Encodes a SaslMessage into a US-ASCII line terminated with CRLF, as required by the D-Bus SASL protocol.
 */
public final class SaslMessageEncoder extends MessageToByteEncoder<SaslMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, SaslMessage msg, ByteBuf out) throws Exception {
    // Write as ASCII followed by CRLF
    out.writeCharSequence(msg.toString(), StandardCharsets.US_ASCII);
    out.writeByte('\r');
    out.writeByte('\n');
  }
}
