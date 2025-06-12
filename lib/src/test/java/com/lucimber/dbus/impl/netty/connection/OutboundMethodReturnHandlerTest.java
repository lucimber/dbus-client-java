/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OutboundMethodReturnHandlerTest {

  @Test
  void encodeSuccessfully() {
    OutboundMessageEncoder handler = new OutboundMessageEncoder();
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    UInt32 serial = UInt32.valueOf(2);
    UInt32 replySerial = UInt32.valueOf(1);
    DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
    Signature sig = null;
    List<DBusType> payload = null;
    OutboundMethodReturn methodReturn = new OutboundMethodReturn(serial, replySerial, dst, sig, payload);

    assertTrue(channel.writeOutbound(methodReturn));
    assertTrue(channel.finish());
    Frame frame = channel.readOutbound();

    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.METHOD_RETURN, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    int bodyLength = 0;
    assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
    Assertions.assertEquals(serial, frame.getSerial(), "Serial number");
  }
}
