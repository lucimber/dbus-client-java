/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class OutboundMethodCallHandlerTest {

  @Test
  void encodeSuccessfully() {
    final OutboundMessageEncoder handler = new OutboundMessageEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
    final UInt32 serialNumber = UInt32.valueOf(1);
    final ObjectPath path = ObjectPath.valueOf("/unit_test");
    final DBusString methodName = DBusString.valueOf("UnitTest");
    final OutboundMethodCall methodCall = new OutboundMethodCall(serialNumber, destination, path, methodName);
    final Signature signature = Signature.valueOf("s");
    methodCall.setSignature(signature);
    final List<DBusType> payload = new ArrayList<>();
    payload.add(DBusString.valueOf("testArg"));
    methodCall.setPayload(payload);
    final DBusString interfaceName = DBusString.valueOf("io.lucimber.test");
    methodCall.setInterfaceName(interfaceName);
    assertTrue(channel.writeOutbound(methodCall));
    assertTrue(channel.finish());
    final Frame frame = channel.readOutbound();
    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    final int bodyLength = 12;
    assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
    assertEquals(serialNumber, frame.getSerial(), "Serial number");
  }

  @Test
  void encodeHelloSuccessfully() {
    final OutboundMessageEncoder handler = new OutboundMessageEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final DBusString destination = DBusString.valueOf("org.freedesktop.DBus");
    final UInt32 serialNumber = UInt32.valueOf(1);
    final ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");
    final DBusString interfaceName = DBusString.valueOf("org.freedesktop.DBus");
    final DBusString methodName = DBusString.valueOf("Hello");
    final OutboundMethodCall methodCall = new OutboundMethodCall(serialNumber, destination, path, methodName);
    methodCall.setInterfaceName(interfaceName);
    assertTrue(channel.writeOutbound(methodCall));
    assertTrue(channel.finish());
    final Frame frame = channel.readOutbound();
    assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    final int bodyLength = 0;
    assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
    assertEquals(serialNumber, frame.getSerial(), "Serial number");
  }
}
