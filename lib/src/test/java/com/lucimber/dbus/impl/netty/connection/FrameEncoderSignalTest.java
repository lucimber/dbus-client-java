/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class FrameEncoderSignalTest {

  @Test
  void encodeSuccessfully() {
    final UInt32 serialNumber = UInt32.valueOf(1);
    final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
    final ObjectPath path = ObjectPath.valueOf("/test");
    final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
    final DBusString signalName = DBusString.valueOf("UnitTest");
    final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    final Frame frame = new Frame();
    frame.setSerial(serialNumber);
    frame.setByteOrder(byteOrder);
    frame.setProtocolVersion(1);
    frame.setType(MessageType.SIGNAL);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant destinationVariant = Variant.valueOf(destination);
    headerFields.put(HeaderField.DESTINATION, destinationVariant);
    final Variant pathVariant = Variant.valueOf(path);
    headerFields.put(HeaderField.PATH, pathVariant);
    final Variant interfaceVariant = Variant.valueOf(interfaceName);
    headerFields.put(HeaderField.INTERFACE, interfaceVariant);
    final Variant memberVariant = Variant.valueOf(signalName);
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
