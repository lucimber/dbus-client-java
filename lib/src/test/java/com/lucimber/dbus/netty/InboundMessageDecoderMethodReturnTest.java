/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.codec.encoder.EncoderResult;
import com.lucimber.dbus.codec.encoder.EncoderResultImpl;
import com.lucimber.dbus.codec.encoder.EncoderUtils;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class InboundMessageDecoderMethodReturnTest {

    private static final int PROTOCOL_VERSION = 1;

    private static EncoderResult<ByteBuffer> encodeFrameBody(
            List<DBusType> args, ByteOrder byteOrder) {
        int localByteCount = 0;
        List<ByteBuffer> values = new ArrayList<>();
        for (DBusType dbusObject : args) {
            final com.lucimber.dbus.codec.encoder.EncoderResult<ByteBuffer> result =
                    EncoderUtils.encode(dbusObject, localByteCount, byteOrder);
            localByteCount += result.getProducedBytes();
            values.add(result.getBuffer());
        }

        ByteBuffer body = ByteBuffer.allocate(localByteCount);
        for (ByteBuffer bb : values) {
            body.put(bb);
        }

        return new EncoderResultImpl<>(localByteCount, body);
    }

    @Test
    void succeedWithSimpleMethodReturn() {
        final InboundMessageDecoder decoder = new InboundMessageDecoder();
        final EmbeddedChannel channel = new EmbeddedChannel(decoder);
        final Frame frame = new Frame();
        frame.setByteOrder(ByteOrder.BIG_ENDIAN);
        frame.setProtocolVersion(PROTOCOL_VERSION);
        frame.setSerial(DBusUInt32.valueOf(1));
        frame.setType(MessageType.METHOD_RETURN);
        final Map<HeaderField, DBusVariant> headerFields = new HashMap<>();
        final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
        final DBusVariant senderVariant = DBusVariant.valueOf(sender);
        headerFields.put(HeaderField.SENDER, senderVariant);
        final DBusUInt32 replySerial = DBusUInt32.valueOf(1);
        final DBusVariant replySerialVariant = DBusVariant.valueOf(replySerial);
        headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
        frame.setHeaderFields(headerFields);
        assertTrue(channel.writeInbound(frame));
        final InboundMethodReturn inboundMessage = channel.readInbound();
        assertEquals(sender, inboundMessage.getSender(), "Sender");
        assertEquals(replySerial, inboundMessage.getReplySerial(), "Reply serial");
    }

    @Test
    void succeedWithComplexMethodReturn() {
        final InboundMessageDecoder decoder = new InboundMessageDecoder();
        final EmbeddedChannel channel = new EmbeddedChannel(decoder);
        final Frame frame = new Frame();
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        frame.setByteOrder(byteOrder);
        frame.setProtocolVersion(PROTOCOL_VERSION);
        frame.setSerial(DBusUInt32.valueOf(1));
        frame.setType(MessageType.METHOD_RETURN);
        final Map<HeaderField, DBusVariant> headerFields = new HashMap<>();
        final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
        final DBusVariant senderVariant = DBusVariant.valueOf(sender);
        headerFields.put(HeaderField.SENDER, senderVariant);
        final DBusUInt32 replySerial = DBusUInt32.valueOf(1);
        final DBusVariant replySerialVariant = DBusVariant.valueOf(replySerial);
        headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
        final DBusSignature signature = DBusSignature.valueOf("a{oa{sa{sv}}}");
        final DBusVariant signatureVariant = DBusVariant.valueOf(signature);
        headerFields.put(HeaderField.SIGNATURE, signatureVariant);
        frame.setHeaderFields(headerFields);
        final DBusDict<DBusObjectPath, DBusDict<DBusString, DBusDict<DBusString, DBusVariant>>>
                dict = new DBusDict<>(signature);
        final List<DBusType> payload = new ArrayList<>();
        payload.add(dict);
        final EncoderResult<ByteBuffer> result = encodeFrameBody(payload, byteOrder);
        frame.setBody(result.getBuffer());
        assertTrue(channel.writeInbound(frame));
        final InboundMethodReturn methodReturn = channel.readInbound();
        assertEquals(sender, methodReturn.getSender(), "Sender");
        assertEquals(replySerial, methodReturn.getReplySerial(), "Reply serial");
    }
}
