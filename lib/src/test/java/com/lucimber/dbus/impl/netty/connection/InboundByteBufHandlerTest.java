/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.impl.netty.encoder.ArrayEncoder;
import com.lucimber.dbus.impl.netty.encoder.Encoder;
import com.lucimber.dbus.impl.netty.encoder.EncoderResult;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class InboundByteBufHandlerTest {

  private static final byte BIG_ENDIAN_MAGIC = 0x42;
  private static final int HEADER_BOUNDARY = 8;
  private static final byte LITTLE_ENDIAN_MAGIC = 0x6C;

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void succeedWithError(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeByte(BIG_ENDIAN_MAGIC);
    } else {
      buffer.writeByte(LITTLE_ENDIAN_MAGIC);
    }
    final byte msgTypeError = 0x03;
    buffer.writeByte(msgTypeError);
    final byte msgFlag = 0x00;
    buffer.writeByte(msgFlag);
    final byte version = 0x01;
    buffer.writeByte(version);
    // Body length
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(0);
    } else {
      buffer.writeIntLE(0);
    }
    // Serial number
    final int serial = 2;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(serial);
    } else {
      buffer.writeIntLE(serial);
    }
    // Header fields
    final Signature signature = Signature.valueOf("a(yv)");
    final DBusArray<Struct> array = new DBusArray<>(signature);
    final Signature structSignature = signature.subContainer();
    final DBusString errorName = DBusString.valueOf("io.lucimber.Error.UnitTest");
    final Variant errorNameVariant = Variant.valueOf(errorName);
    final Struct errorStruct = new Struct(structSignature, DBusByte.valueOf((byte) 4), errorNameVariant);
    array.add(errorStruct);
    final UInt32 replySerial = UInt32.valueOf(1);
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    final Struct replySerialStruct = new Struct(structSignature, DBusByte.valueOf((byte) 5), replySerialVariant);
    array.add(replySerialStruct);
    final Encoder<DBusArray<Struct>, ByteBuf> headerEncoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> headerResult = headerEncoder.encode(array, 12);
    final ByteBuf headerBuffer = headerResult.getBuffer();
    buffer.writeBytes(headerBuffer);
    headerBuffer.release();
    addAlignmentPaddingAfterHeaderIfNecessary(buffer);
    final ByteBufDecoder inboundHandler = new ByteBufDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    assertTrue(channel.writeInbound(buffer));
    final Frame frame = channel.readInbound();
    assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
    assertTrue(frame.getFlags().isEmpty(), "Message flags");
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
    assertEquals(MessageType.ERROR, frame.getType(), "Message type");
    assertEquals(0, frame.getBody().readableBytes(), "Frame body");
    frame.getBody().release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void succeedWithMethodCall(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeByte(BIG_ENDIAN_MAGIC);
    } else {
      buffer.writeByte(LITTLE_ENDIAN_MAGIC);
    }
    final byte msgTypeMethodCall = 0x01;
    buffer.writeByte(msgTypeMethodCall);
    final byte msgFlag = 0x00;
    buffer.writeByte(msgFlag);
    final byte version = 0x01;
    buffer.writeByte(version);
    // Body length
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(0);
    } else {
      buffer.writeIntLE(0);
    }
    // Serial number
    final int serial = 1;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(serial);
    } else {
      buffer.writeIntLE(serial);
    }
    // Header fields
    final Signature signature = Signature.valueOf("a(yv)");
    final DBusArray<Struct> structList = new DBusArray<>(signature);
    final Signature structSignature = signature.subContainer();
    final DBusString methodName = DBusString.valueOf("MethodCall");
    final Variant memberVariant = Variant.valueOf(methodName);
    final Struct memberStruct = new Struct(structSignature, DBusByte.valueOf((byte) 3), memberVariant);
    structList.add(memberStruct);
    final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
    final Variant interfaceVariant = Variant.valueOf(interfaceName);
    final Struct interfaceStruct = new Struct(structSignature, DBusByte.valueOf((byte) 2), interfaceVariant);
    structList.add(interfaceStruct);
    final ObjectPath path = ObjectPath.valueOf("/");
    final Variant pathVariant = Variant.valueOf(path);
    final Struct pathStruct = new Struct(structSignature, DBusByte.valueOf((byte) 1), pathVariant);
    structList.add(pathStruct);
    final Encoder<DBusArray<Struct>, ByteBuf> headerEncoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> headerResult = headerEncoder.encode(structList, 12);
    final ByteBuf headerBuffer = headerResult.getBuffer();
    buffer.writeBytes(headerBuffer);
    headerBuffer.release();
    addAlignmentPaddingAfterHeaderIfNecessary(buffer);
    final ByteBufDecoder inboundHandler = new ByteBufDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    assertTrue(channel.writeInbound(buffer));
    final Frame frame = channel.readInbound();
    assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
    assertTrue(frame.getFlags().isEmpty(), "Message flags");
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
    assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
    assertEquals(0, frame.getBody().readableBytes(), "Frame body");
    frame.getBody().release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void succeedWithMethodReturnMessage(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeByte(BIG_ENDIAN_MAGIC);
    } else {
      buffer.writeByte(LITTLE_ENDIAN_MAGIC);
    }
    final byte msgTypeMethodReturn = 0x02;
    buffer.writeByte(msgTypeMethodReturn);
    final byte msgFlag = 0x00;
    buffer.writeByte(msgFlag);
    final byte version = 0x01;
    buffer.writeByte(version);
    // Body length
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(0);
    } else {
      buffer.writeIntLE(0);
    }
    // Serial number
    final int serial = 2;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(serial);
    } else {
      buffer.writeIntLE(serial);
    }
    // Header fields
    final Signature signature = Signature.valueOf("a(yv)");
    final DBusArray<Struct> array = new DBusArray<>(signature);
    final Signature structSignature = signature.subContainer();
    final UInt32 replySerial = UInt32.valueOf(1);
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    final Struct replySerialStruct = new Struct(structSignature, DBusByte.valueOf((byte) 5), replySerialVariant);
    array.add(replySerialStruct);
    final Encoder<DBusArray<Struct>, ByteBuf> headerEncoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> headerResult = headerEncoder.encode(array, 12);
    final ByteBuf headerBuffer = headerResult.getBuffer();
    buffer.writeBytes(headerBuffer);
    headerBuffer.release();
    addAlignmentPaddingAfterHeaderIfNecessary(buffer);
    final ByteBufDecoder inboundHandler = new ByteBufDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    assertTrue(channel.writeInbound(buffer));
    final Frame frame = channel.readInbound();
    assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
    assertTrue(frame.getFlags().isEmpty(), "Message flags");
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
    assertEquals(MessageType.METHOD_RETURN, frame.getType(), "Message type");
    assertEquals(0, frame.getBody().readableBytes(), "Frame body");
    frame.getBody().release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void succeedWithSignalMessage(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeByte(BIG_ENDIAN_MAGIC);
    } else {
      buffer.writeByte(LITTLE_ENDIAN_MAGIC);
    }
    final byte msgTypeSignal = 0x04;
    buffer.writeByte(msgTypeSignal);
    final byte msgFlag = 0x00;
    buffer.writeByte(msgFlag);
    final byte version = 0x01;
    buffer.writeByte(version);
    // Body length
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(0);
    } else {
      buffer.writeIntLE(0);
    }
    // Serial number
    final int serial = 2;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(serial);
    } else {
      buffer.writeIntLE(serial);
    }
    // Header fields
    final Signature signature = Signature.valueOf("a(yv)");
    final DBusArray<Struct> array = new DBusArray<>(signature);
    final Signature structSignature = signature.subContainer();
    final DBusString signalName = DBusString.valueOf("TestSignal");
    final Variant memberVariant = Variant.valueOf(signalName);
    final Struct memberStruct = new Struct(structSignature, DBusByte.valueOf((byte) 3), memberVariant);
    array.add(memberStruct);
    final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
    final Variant interfaceVariant = Variant.valueOf(interfaceName);
    final Struct interfaceStruct = new Struct(structSignature, DBusByte.valueOf((byte) 2), interfaceVariant);
    array.add(interfaceStruct);
    final ObjectPath path = ObjectPath.valueOf("/");
    final Variant pathVariant = Variant.valueOf(path);
    final Struct pathStruct = new Struct(structSignature, DBusByte.valueOf((byte) 1), pathVariant);
    array.add(pathStruct);
    final Encoder<DBusArray<Struct>, ByteBuf> headerEncoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> headerResult = headerEncoder.encode(array, 12);
    final ByteBuf headerBuffer = headerResult.getBuffer();
    buffer.writeBytes(headerBuffer);
    headerBuffer.release();
    addAlignmentPaddingAfterHeaderIfNecessary(buffer);
    final ByteBufDecoder inboundHandler = new ByteBufDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    assertTrue(channel.writeInbound(buffer));
    final Frame frame = channel.readInbound();
    assertEquals(byteOrder, frame.getByteOrder(), "Byte order");
    assertTrue(frame.getFlags().isEmpty(), "Message flags");
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    assertEquals(serial, frame.getSerial().getDelegate(), "Serial number");
    assertEquals(MessageType.SIGNAL, frame.getType(), "Message type");
    assertEquals(0, frame.getBody().readableBytes(), "Frame body");
    frame.getBody().release();
  }

  private void addAlignmentPaddingAfterHeaderIfNecessary(final ByteBuf frame) {
    final int remainder = frame.readableBytes() % HEADER_BOUNDARY;
    if (remainder > 0) {
      final int padding = HEADER_BOUNDARY - remainder;
      frame.writeZero(padding);
    }
  }
}
