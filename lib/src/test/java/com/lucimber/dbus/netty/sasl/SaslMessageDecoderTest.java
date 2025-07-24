/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty.sasl;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

class SaslMessageDecoderTest {

    @Test
    void testDecodeDataCommand() {
        var channel = new EmbeddedChannel(new SaslMessageDecoder());
        var input =
                Unpooled.copiedBuffer(
                        "DATA abc123\r\n", java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue(channel.writeInbound(input));
        SaslMessage msg = channel.readInbound();
        assertNotNull(msg);
        assertEquals(SaslCommandName.DATA, msg.getCommandName());
        assertEquals("abc123", msg.getCommandArgs().orElse(null));
    }

    @Test
    void testDecodeOkCommandWithArg() {
        var channel = new EmbeddedChannel(new SaslMessageDecoder());
        var input =
                Unpooled.copiedBuffer(
                        "OK 1234567890\r\n", java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue(channel.writeInbound(input));
        SaslMessage msg = channel.readInbound();
        assertEquals(SaslCommandName.OK, msg.getCommandName());
        assertEquals("1234567890", msg.getCommandArgs().orElse(null));
    }

    @Test
    void testDecodeRejectedCommandWithArgs() {
        var channel = new EmbeddedChannel(new SaslMessageDecoder());
        var input =
                Unpooled.copiedBuffer(
                        "REJECTED EXTERNAL ANONYMOUS\r\n",
                        java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue(channel.writeInbound(input));
        SaslMessage msg = channel.readInbound();
        assertEquals(SaslCommandName.REJECTED, msg.getCommandName());
        assertEquals("EXTERNAL ANONYMOUS", msg.getCommandArgs().orElse(null));
    }

    @Test
    void testDecodeAgreeUnixFd() {
        var channel = new EmbeddedChannel(new SaslMessageDecoder());
        var input =
                Unpooled.copiedBuffer(
                        "AGREE_UNIX_FD\r\n", java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue(channel.writeInbound(input));
        SaslMessage msg = channel.readInbound();
        assertEquals(SaslCommandName.AGREE_UNIX_FD, msg.getCommandName());
        assertTrue(msg.getCommandArgs().isEmpty());
    }

    @Test
    void testUnknownCommandThrowsException() {
        var channel = new EmbeddedChannel(new SaslMessageDecoder());
        var input =
                Unpooled.copiedBuffer(
                        "FOOBAR something\r\n", java.nio.charset.StandardCharsets.US_ASCII);
        assertThrows(DecoderException.class, () -> channel.writeInbound(input));
    }

    @Test
    void testIncompleteLineWaitsForMoreData() {
        var channel = new EmbeddedChannel(new SaslMessageDecoder());
        var input =
                Unpooled.copiedBuffer(
                        "DATA incomplete", java.nio.charset.StandardCharsets.US_ASCII);
        assertFalse(channel.writeInbound(input));
        assertNull(channel.readInbound());
    }
}
