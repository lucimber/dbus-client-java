/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class FrameEncoderMethodReturnTest {

  @Test
  void encodeSuccessfully() {
    final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
    final UInt32 serialNumber = UInt32.valueOf(2);
    final UInt32 replySerialNumber = UInt32.valueOf(1);
    final Frame frame = new Frame();
    frame.setSerial(serialNumber);
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(1);
    frame.setType(MessageType.METHOD_RETURN);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant replySerialVariant = Variant.valueOf(replySerialNumber);
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    final Variant destinationVariant = Variant.valueOf(destination);
    headerFields.put(HeaderField.DESTINATION, destinationVariant);
    frame.setHeaderFields(headerFields);
    final FrameEncoder handler = new FrameEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    assertTrue(channel.writeOutbound(frame));
    assertTrue(channel.finish());
    final ByteBuf buffer = channel.readOutbound();
    final byte bigEndian = 0x42;
    assertEquals(bigEndian, buffer.readByte(), "Byte order");
    final byte msgType = 0x02;
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
