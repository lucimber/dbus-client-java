/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class OutboundMethodCallHandlerTest {

    @Test
    void encodeSuccessfully() {
        OutboundMessageEncoder handler = new OutboundMessageEncoder();
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        DBusUInt32 serial = DBusUInt32.valueOf(1);
        DBusObjectPath path = DBusObjectPath.valueOf("/unit_test");
        DBusString member = DBusString.valueOf("UnitTest");
        boolean replyExpected = true;
        DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
        DBusString iface = DBusString.valueOf("io.lucimber.test");
        DBusSignature sig = DBusSignature.valueOf("s");
        List<DBusType> payload = new ArrayList<>();
        payload.add(DBusString.valueOf("testArg"));
        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withPath(path)
                        .withMember(member)
                        .withReplyExpected(replyExpected)
                        .withDestination(dst)
                        .withInterface(iface)
                        .withBody(sig, payload)
                        .build();

        assertTrue(channel.writeOutbound(methodCall));
        assertTrue(channel.finish());
        Frame frame = channel.readOutbound();

        Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
        Assertions.assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
        assertTrue(frame.getFlags().isEmpty());
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        int bodyLength = 12;
        assertEquals(bodyLength, frame.getBody().remaining(), "Body length");
        assertEquals(serial, frame.getSerial(), "Serial number");
    }

    @Test
    void encodeHelloSuccessfully() {
        OutboundMessageEncoder handler = new OutboundMessageEncoder();
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        DBusUInt32 serial = DBusUInt32.valueOf(1);
        DBusObjectPath path = DBusObjectPath.valueOf("/org/freedesktop/DBus");

        DBusString member = DBusString.valueOf("Hello");
        DBusString dst = DBusString.valueOf("org.freedesktop.DBus");
        DBusString iface = DBusString.valueOf("org.freedesktop.DBus");
        OutboundMethodCall methodCall =
                OutboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withPath(path)
                        .withMember(member)
                        .withDestination(dst)
                        .withInterface(iface)
                        .build();

        assertTrue(channel.writeOutbound(methodCall));
        assertTrue(channel.finish());
        Frame frame = channel.readOutbound();

        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
        Assertions.assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
        assertTrue(frame.getFlags().isEmpty());
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        int bodyLength = 0;
        assertEquals(bodyLength, frame.getBody().remaining(), "Body length");
        assertEquals(serial, frame.getSerial(), "Serial number");
    }
}
