/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.impl.netty.encoder.Encoder;
import com.lucimber.dbus.impl.netty.encoder.EncoderResult;
import com.lucimber.dbus.impl.netty.encoder.StringEncoder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.protocol.types.DBusString;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.protocol.types.UInt32;
import com.lucimber.dbus.protocol.types.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class FrameEncoderErrorMessageTest {

  @Test
  void encodeSuccessfully() {
    final FrameEncoder handler = new FrameEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
    final UInt32 replySerial = UInt32.valueOf(1);
    final UInt32 serial = UInt32.valueOf(7);
    final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
    final Frame frame = new Frame();
    frame.setSerial(serial);
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(1);
    frame.setType(MessageType.ERROR);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    final Variant destinationVariant = Variant.valueOf(destination);
    headerFields.put(HeaderField.DESTINATION, destinationVariant);
    final Variant errorNameVariant = Variant.valueOf(errorName);
    headerFields.put(HeaderField.ERROR_NAME, errorNameVariant);
    frame.setHeaderFields(headerFields);
    assertTrue(channel.writeOutbound(frame));
    assertTrue(channel.finish());
    final ByteBuf buffer = channel.readOutbound();
    final byte bigEndian = 0x42;
    assertEquals(bigEndian, buffer.readByte());
    final byte msgType = 0x03;
    assertEquals(msgType, buffer.readByte());
    final int skipFlagByte = 1;
    buffer.skipBytes(skipFlagByte);
    final byte protocolVersion = 0x01;
    assertEquals(protocolVersion, buffer.readByte());
    final int bodyLength = 0;
    assertEquals(bodyLength, buffer.readInt());
    assertEquals(serial.getDelegate(), buffer.readInt());
    buffer.release();
  }

  @Test
  void encodeWithErrorMessageSuccessfully() {
    final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    final DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
    final DBusString errorMsg = DBusString.valueOf("Test error message.");
    final UInt32 replySerial = UInt32.valueOf(1);
    final UInt32 serial = UInt32.valueOf(7);
    final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
    final Signature signature = Signature.valueOf("s");
    final Frame frame = new Frame();
    frame.setSerial(serial);
    frame.setByteOrder(byteOrder);
    frame.setProtocolVersion(1);
    frame.setType(MessageType.ERROR);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    final Variant destinationVariant = Variant.valueOf(destination);
    headerFields.put(HeaderField.DESTINATION, destinationVariant);
    final Variant errorNameVariant = Variant.valueOf(errorName);
    headerFields.put(HeaderField.ERROR_NAME, errorNameVariant);
    final Variant signatureVariant = Variant.valueOf(signature);
    headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    frame.setHeaderFields(headerFields);
    final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final EncoderResult<ByteBuf> encoderResult = encoder.encode(errorMsg, 0);
    frame.setBody(encoderResult.getBuffer());
    final FrameEncoder handler = new FrameEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    assertTrue(channel.writeOutbound(frame));
    assertTrue(channel.finish());
    final ByteBuf buffer = channel.readOutbound();
    final byte bigEndian = 0x42;
    assertEquals(bigEndian, buffer.readByte(), "Byte order");
    final byte msgType = 0x03;
    assertEquals(msgType, buffer.readByte(), "Message type");
    final int skipFlagByte = 1;
    buffer.skipBytes(skipFlagByte);
    final byte protocolVersion = 0x01;
    assertEquals(protocolVersion, buffer.readByte(), "Protocol version");
    final int bodyLength = 24;
    assertEquals(bodyLength, buffer.readInt(), "Body length");
    assertEquals(serial.getDelegate(), buffer.readInt(), "Serial number");
    buffer.release();
  }
}
