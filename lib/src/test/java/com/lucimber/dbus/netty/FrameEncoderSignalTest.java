/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class FrameEncoderSignalTest {

    @Test
    void encodeSuccessfully() {
        final DBusUInt32 serialNumber = DBusUInt32.valueOf(1);
        final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        final DBusObjectPath path = DBusObjectPath.valueOf("/test");
        final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
        final DBusString signalName = DBusString.valueOf("UnitTest");
        final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
        final Frame frame = new Frame();
        frame.setSerial(serialNumber);
        frame.setByteOrder(byteOrder);
        frame.setProtocolVersion(1);
        frame.setType(MessageType.SIGNAL);
        final Map<HeaderField, DBusVariant> headerFields = new HashMap<>();
        final DBusVariant destinationVariant = DBusVariant.valueOf(destination);
        headerFields.put(HeaderField.DESTINATION, destinationVariant);
        final DBusVariant pathVariant = DBusVariant.valueOf(path);
        headerFields.put(HeaderField.PATH, pathVariant);
        final DBusVariant interfaceVariant = DBusVariant.valueOf(interfaceName);
        headerFields.put(HeaderField.INTERFACE, interfaceVariant);
        final DBusVariant memberVariant = DBusVariant.valueOf(signalName);
        headerFields.put(HeaderField.MEMBER, memberVariant);
        frame.setHeaderFields(headerFields);
        final FrameEncoder handler = new FrameEncoder();
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        assertTrue(channel.writeOutbound(frame));
        assertTrue(channel.finish());
        final ByteBuf buffer = channel.readOutbound();
        final byte bigEndian = 0x42;
        assertEquals(bigEndian, buffer.readByte(), "Byte order");
        final byte msgType = 0x04;
        assertEquals(msgType, buffer.readByte(), "Message type");
        final int skipFlagByte = 1;
        buffer.skipBytes(skipFlagByte);
        final byte protocolVersion = 0x01;
        assertEquals(protocolVersion, buffer.readByte(), "Protocol version");
        final int bodyLength = 0;
        assertEquals(bodyLength, buffer.readInt(), "Body length");
        assertEquals(serialNumber.getDelegate(), buffer.readInt(), "Serial number");
        buffer.release();
    }
}
