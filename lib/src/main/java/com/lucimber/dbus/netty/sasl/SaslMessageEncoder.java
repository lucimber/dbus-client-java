/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.util.LoggerUtils;

/**
 * Encodes a SaslMessage into a US-ASCII line terminated with CRLF, as required by the D-Bus SASL
 * protocol.
 */
public final class SaslMessageEncoder extends MessageToByteEncoder<SaslMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaslMessageEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, SaslMessage msg, ByteBuf out)
            throws Exception {
        // Write as ASCII followed by CRLF
        out.writeCharSequence(msg.toString(), StandardCharsets.US_ASCII);
        out.writeByte('\r');
        out.writeByte('\n');

        LOGGER.debug(LoggerUtils.SASL, "Encoded {}", msg);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        LOGGER.debug(
                LoggerUtils.HANDLER_LIFECYCLE,
                "Added to pipeline using context name '{}'.",
                ctx.name());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        LOGGER.debug(
                LoggerUtils.HANDLER_LIFECYCLE,
                "Removed from pipeline using context name '{}'.",
                ctx.name());
    }
}
