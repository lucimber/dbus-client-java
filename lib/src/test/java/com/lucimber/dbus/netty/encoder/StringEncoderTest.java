/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.netty.ByteOrder;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class StringEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeSimpleString(final ByteOrder byteOrder) {
    final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final DBusString dbusString = DBusString.valueOf("abcABC_äüö");
    final EncoderResult<ByteBuf> result = encoder.encode(dbusString, 0);
    final int expectedNumOfBytes = 18;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    // UINT32 bytes
    final int numOfLengthBytes = 4;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0D};
      final byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.readBytes(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      final byte[] expectedBytes = {0x0D, 0x00, 0x00, 0x00};
      final byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.readBytes(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    // UTF-8 bytes (13)
    final int numOfRemainingBytes = 13;
    buffer.skipBytes(numOfRemainingBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeSimpleStringWithOffset(final ByteOrder byteOrder) {
    final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final DBusString dbusString = DBusString.valueOf("abcABC_äüö");
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(dbusString, offset);
    final int expectedNumOfBytes = 21;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final int numOfOffsetBytes = 3;
    buffer.skipBytes(numOfOffsetBytes);
    // UINT32 bytes
    final int numOfLengthBytes = 4;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0D};
      final byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.readBytes(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      final byte[] expectedBytes = {0x0D, 0x00, 0x00, 0x00};
      final byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.readBytes(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    // UTF-8 bytes (13)
    final int numOfRemainingBytes = 13;
    buffer.skipBytes(numOfRemainingBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeEmptyString(final ByteOrder byteOrder) {
    final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final DBusString dbusString = DBusString.valueOf("");
    final EncoderResult<ByteBuf> result = encoder.encode(dbusString, 0);
    final int expectedNumOfBytes = 5;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    // UINT32
    final int numOfLengthBytes = 4;
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00};
    final byte[] actualBytes = new byte[numOfLengthBytes];
    buffer.readBytes(actualBytes, 0, numOfLengthBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeStringOfObjectPath(final ByteOrder byteOrder) {
    final ObjectPath objectPath = ObjectPath.valueOf("/abc/d1/e_f");
    final DBusString rawPath = DBusString.valueOf(objectPath.getWrappedValue().toString());
    final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final EncoderResult<ByteBuf> result = encoder.encode(rawPath, 0);
    final int expectedNumOfBytes = 16;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    // UINT32 bytes
    final int numOfLengthBytes = 4;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0B};
      final byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.readBytes(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      final byte[] expectedBytes = {0x0B, 0x00, 0x00, 0x00};
      final byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.readBytes(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    // UTF-8 bytes (11 bytes)
    final int numOfRemainingBytes = 11;
    buffer.skipBytes(numOfRemainingBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
    buffer.release();
  }
}
