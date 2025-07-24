/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Decoder: Accumulates ASCII bytes until CRLF, then maps to SaslMessage. */
public class SaslMessageDecoder extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaslMessageDecoder.class);

    private static int findEndOfLine(ByteBuf buffer) {
        LOGGER.trace("Finding end of line (CRLF) in buffer.");

        final int start = buffer.readerIndex();
        final int end = buffer.writerIndex();

        for (int i = start; i < end - 1; i++) {
            if (buffer.getByte(i) == '\r' && buffer.getByte(i + 1) == '\n') {
                return i + 1; // return index of '\n'
            }
        }
        return -1;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        LOGGER.debug(
                LoggerUtils.HANDLER_LIFECYCLE,
                "Added to pipeline using context name '{}'.",
                ctx.name());
    }

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) {
        LOGGER.debug(
                LoggerUtils.HANDLER_LIFECYCLE,
                "Removed from pipeline using context name '{}'.",
                ctx.name());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int eolIndex = findEndOfLine(in);
        if (eolIndex == -1) {
            return; // Wait for full line
        }

        int crIndex = eolIndex - 1;
        int length = crIndex - in.readerIndex();
        ByteBuf lineBuf = in.readRetainedSlice(length);
        in.skipBytes(2); // Skip CRLF
        String line = lineBuf.toString(StandardCharsets.US_ASCII);
        lineBuf.release();

        SaslMessage msg = mapToSaslMessage(line);
        LOGGER.debug(LoggerUtils.SASL, "Decoded {}", msg);

        out.add(msg);
    }

    private SaslMessage mapToSaslMessage(String line) {
        if (line.startsWith("DATA ")) {
            String cmdValue = line.substring(5);
            return new SaslMessage(SaslCommandName.DATA, cmdValue);
        } else if (line.startsWith("REJECTED")) {
            String cmdValue = line.length() > 8 ? line.substring(8).trim() : "";
            return new SaslMessage(SaslCommandName.REJECTED, cmdValue.isEmpty() ? null : cmdValue);
        } else if (line.startsWith("OK ")) {
            String cmdValue = line.substring(3);
            return new SaslMessage(SaslCommandName.OK, cmdValue.isEmpty() ? null : cmdValue);
        } else if (line.startsWith("ERROR")) {
            String cmdValue = line.length() > 5 ? line.substring(5).trim() : "";
            return new SaslMessage(SaslCommandName.ERROR, cmdValue.isEmpty() ? null : cmdValue);
        } else if (line.startsWith("AGREE_UNIX_FD")) {
            return new SaslMessage(SaslCommandName.AGREE_UNIX_FD, null);
        } else {
            throw new DecoderException("Unknown or misdirected SASL message: " + line);
        }
    }
}
