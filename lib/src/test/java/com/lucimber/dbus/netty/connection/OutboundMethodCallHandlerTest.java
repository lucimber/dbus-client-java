/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OutboundMethodCallHandlerTest {

  @Test
  void encodeSuccessfully() {
    OutboundMessageEncoder handler = new OutboundMessageEncoder();
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    UInt32 serial = UInt32.valueOf(1);
    ObjectPath path = ObjectPath.valueOf("/unit_test");
    DBusString member = DBusString.valueOf("UnitTest");
    boolean replyExpected = false;
    DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
    DBusString iface = DBusString.valueOf("io.lucimber.test");
    Signature sig = Signature.valueOf("s");
    List<DBusType> payload = new ArrayList<>();
    payload.add(DBusString.valueOf("testArg"));
    OutboundMethodCall methodCall = new OutboundMethodCall(serial, path, member, replyExpected,
          dst, iface, sig, payload);

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


    UInt32 serial = UInt32.valueOf(1);
    ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");

    DBusString member = DBusString.valueOf("Hello");
    boolean replyExpected = false;
    DBusString dst = DBusString.valueOf("org.freedesktop.DBus");
    DBusString iface = DBusString.valueOf("org.freedesktop.DBus");
    Signature sig = null;
    List<DBusType> payload = null;
    OutboundMethodCall methodCall = new OutboundMethodCall(serial, path, member, replyExpected,
          dst, iface, sig, payload);

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
