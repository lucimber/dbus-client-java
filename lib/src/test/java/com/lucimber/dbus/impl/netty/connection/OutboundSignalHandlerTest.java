/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundSignal;
import com.lucimber.dbus.protocol.types.DBusString;
import com.lucimber.dbus.protocol.types.ObjectPath;
import com.lucimber.dbus.protocol.types.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class OutboundSignalHandlerTest {

  @Test
  void encodeSuccessfully() {
    final OutboundMessageEncoder handler = new OutboundMessageEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final UInt32 serialNumber = UInt32.valueOf(1);
    final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
    final ObjectPath path = ObjectPath.valueOf("/test");
    final DBusString signalName = DBusString.valueOf("UnitTest");
    final OutboundSignal signal = new OutboundSignal(serialNumber, path, interfaceName, signalName);
    assertTrue(channel.writeOutbound(signal));
    assertTrue(channel.finish());
    final Frame frame = channel.readOutbound();
    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.SIGNAL, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    final int bodyLength = 0;
    assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
    assertEquals(serialNumber, frame.getSerial(), "Serial number");
  }
}
