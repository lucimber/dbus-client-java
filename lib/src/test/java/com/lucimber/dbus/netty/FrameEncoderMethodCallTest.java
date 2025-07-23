/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.lucimber.dbus.codec.encoder.Encoder;
import com.lucimber.dbus.codec.encoder.EncoderResult;
import com.lucimber.dbus.codec.encoder.StringEncoder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrameEncoderMethodCallTest {

    @BeforeEach
    void resetDiagnosticContext() {
        MDC.clear();
    }

    @Test
    void encodeSuccessfully() {
        final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        final DBusUInt32 serial = DBusUInt32.valueOf(1);
        final DBusObjectPath path = DBusObjectPath.valueOf("/unit_test");
        final DBusString interfaceName = DBusString.valueOf("io.lucimber.test");
        final DBusString methodName = DBusString.valueOf("UnitTest");
        final DBusString methodArg = DBusString.valueOf("testArg");
        final DBusSignature signature = DBusSignature.valueOf("s");
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        final Frame frame = new Frame();
        frame.setSerial(serial);
        frame.setByteOrder(byteOrder);
        frame.setProtocolVersion(1);
        frame.setType(MessageType.METHOD_CALL);
        final Map<HeaderField, DBusVariant> headerFields = new HashMap<>();
        final DBusVariant destinationVariant = DBusVariant.valueOf(destination);
        headerFields.put(HeaderField.DESTINATION, destinationVariant);
        final DBusVariant pathVariant = DBusVariant.valueOf(path);
        headerFields.put(HeaderField.PATH, pathVariant);
        final DBusVariant interfaceVariant = DBusVariant.valueOf(interfaceName);
        headerFields.put(HeaderField.INTERFACE, interfaceVariant);
        final DBusVariant memberVariant = DBusVariant.valueOf(methodName);
        headerFields.put(HeaderField.MEMBER, memberVariant);
        final DBusVariant signatureVariant = DBusVariant.valueOf(signature);
        headerFields.put(HeaderField.SIGNATURE, signatureVariant);
        frame.setHeaderFields(headerFields);
        final Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
        final EncoderResult<ByteBuffer> encoderResult = encoder.encode(methodArg, 0);
        frame.setBody(encoderResult.getBuffer());
        final FrameEncoder handler = new FrameEncoder();
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        assertTrue(channel.writeOutbound(frame));
        assertTrue(channel.finish());
        final ByteBuf buffer = channel.readOutbound();
        final byte bigEndian = 0x42;
        assertEquals(bigEndian, buffer.readByte(), "Byte order");
        final byte msgType = 0x01;
        assertEquals(msgType, buffer.readByte(), "Message type");
        final int skipFlagByte = 1;
        buffer.skipBytes(skipFlagByte);
        final byte protocolVersion = 0x01;
        assertEquals(protocolVersion, buffer.readByte(), "Protocol version");
        final int bodyLength = 12;
        assertEquals(bodyLength, buffer.readInt(), "Body length");
        assertEquals(serial.getDelegate(), buffer.readInt(), "Serial number");
        buffer.release();
    }

    @Test
    void encodeHelloSuccessfully() {
        final DBusString destination = DBusString.valueOf("org.freedesktop.DBus");
        final DBusUInt32 serial = DBusUInt32.valueOf(1);
        final DBusObjectPath path = DBusObjectPath.valueOf("/org/freedesktop/DBus");
        final DBusString interfaceName = DBusString.valueOf("org.freedesktop.DBus");
        final DBusString methodName = DBusString.valueOf("Hello");
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        final Frame frame = new Frame();
        frame.setSerial(serial);
        frame.setByteOrder(byteOrder);
        frame.setProtocolVersion(1);
        frame.setType(MessageType.METHOD_CALL);
        final Map<HeaderField, DBusVariant> headerFields = new HashMap<>();
        final DBusVariant destinationVariant = DBusVariant.valueOf(destination);
        headerFields.put(HeaderField.DESTINATION, destinationVariant);
        final DBusVariant pathVariant = DBusVariant.valueOf(path);
        headerFields.put(HeaderField.PATH, pathVariant);
        final DBusVariant interfaceVariant = DBusVariant.valueOf(interfaceName);
        headerFields.put(HeaderField.INTERFACE, interfaceVariant);
        final DBusVariant memberVariant = DBusVariant.valueOf(methodName);
        headerFields.put(HeaderField.MEMBER, memberVariant);
        frame.setHeaderFields(headerFields);
        final FrameEncoder handler = new FrameEncoder();
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        assertTrue(channel.writeOutbound(frame));
        assertTrue(channel.finish());
        final ByteBuf buffer = channel.readOutbound();
        final byte bigEndian = 0x42;
        assertEquals(bigEndian, buffer.readByte(), "Byte order");
        final byte msgType = 0x01;
        assertEquals(msgType, buffer.readByte(), "Message type");
        final int flagByte = 0;
        assertEquals(flagByte, buffer.readByte(), "Flags byte");
        final byte protocolVersion = 0x01;
        assertEquals(protocolVersion, buffer.readByte(), "Protocol version");
        final int bodyLength = 0;
        assertEquals(bodyLength, buffer.readInt(), "Body length");
        assertEquals(serial.getDelegate(), buffer.readInt(), "Serial number");
        buffer.release();
    }
}
