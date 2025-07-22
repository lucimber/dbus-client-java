/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.codec.encoder.Encoder;
import com.lucimber.dbus.codec.encoder.EncoderResult;
import com.lucimber.dbus.codec.encoder.StringEncoder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrameEncoderErrorMessageTest {

  @Test
  void encodeSuccessfully() {
  final FrameEncoder handler = new FrameEncoder();
  final EmbeddedChannel channel = new EmbeddedChannel(handler);
  final DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
  final DBusUInt32 replySerial = DBusUInt32.valueOf(1);
  final DBusUInt32 serial = DBusUInt32.valueOf(7);
  final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
  final Frame frame = new Frame();
  frame.setSerial(serial);
  frame.setByteOrder(ByteOrder.BIG_ENDIAN);
  frame.setProtocolVersion(1);
  frame.setType(MessageType.ERROR);
  final Map<HeaderField, DBusVariant> headerFields = new HashMap<>();
  final DBusVariant replySerialVariant = DBusVariant.valueOf(replySerial);
  headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
  final DBusVariant destinationVariant = DBusVariant.valueOf(destination);
  headerFields.put(HeaderField.DESTINATION, destinationVariant);
  final DBusVariant errorNameVariant = DBusVariant.valueOf(errorName);
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
  final DBusUInt32 replySerial = DBusUInt32.valueOf(1);
  final DBusUInt32 serial = DBusUInt32.valueOf(7);
  final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
  final DBusSignature signature = DBusSignature.valueOf("s");
  final Frame frame = new Frame();
  frame.setSerial(serial);
  frame.setByteOrder(byteOrder);
  frame.setProtocolVersion(1);
  frame.setType(MessageType.ERROR);
  final Map<HeaderField, DBusVariant> headerFields = new HashMap<>();
  final DBusVariant replySerialVariant = DBusVariant.valueOf(replySerial);
  headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
  final DBusVariant destinationVariant = DBusVariant.valueOf(destination);
  headerFields.put(HeaderField.DESTINATION, destinationVariant);
  final DBusVariant errorNameVariant = DBusVariant.valueOf(errorName);
  headerFields.put(HeaderField.ERROR_NAME, errorNameVariant);
  final DBusVariant signatureVariant = DBusVariant.valueOf(signature);
  headerFields.put(HeaderField.SIGNATURE, signatureVariant);
  frame.setHeaderFields(headerFields);
  final Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
  final EncoderResult<ByteBuffer> encoderResult = encoder.encode(errorMsg, 0);
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
