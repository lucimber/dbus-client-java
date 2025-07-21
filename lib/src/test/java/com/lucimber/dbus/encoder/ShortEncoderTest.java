/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusInt16;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShortEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSignedShortMaxValue(ByteOrder byteOrder) {
  Encoder<DBusInt16, ByteBuffer> encoder = new Int16Encoder(byteOrder);
  DBusInt16 int16 = DBusInt16.valueOf(Short.MAX_VALUE);
  EncoderResult<ByteBuffer> result = encoder.encode(int16, 0);
  int expectedNumOfBytes = 2;
  assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x7F, (byte) 0xFF};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  } else {
      byte[] expectedBytes = {(byte) 0xFF, 0x7F};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSignedShortMaxValueWithOffset(ByteOrder byteOrder) {
  Encoder<DBusInt16, ByteBuffer> encoder = new Int16Encoder(byteOrder);
  DBusInt16 int16 = DBusInt16.valueOf(Short.MAX_VALUE);
  int offset = 5;
  EncoderResult<ByteBuffer> result = encoder.encode(int16, offset);
  int expectedNumOfBytes = 3;
  assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x00, 0x7F, (byte) 0xFF};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  } else {
      byte[] expectedBytes = {0x00, (byte) 0xFF, 0x7F};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSignedShortMinValue(ByteOrder byteOrder) {
  Encoder<DBusInt16, ByteBuffer> encoder = new Int16Encoder(byteOrder);
  DBusInt16 int16 = DBusInt16.valueOf(Short.MIN_VALUE);
  EncoderResult<ByteBuffer> result = encoder.encode(int16, 0);
  int expectedNumOfBytes = 2;
  assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {(byte) 0x80, 0x00};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  } else {
      byte[] expectedBytes = {0x00, (byte) 0x80};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSignedShortMinValueWithOffset(ByteOrder byteOrder) {
  Encoder<DBusInt16, ByteBuffer> encoder = new Int16Encoder(byteOrder);
  DBusInt16 int16 = DBusInt16.valueOf(Short.MIN_VALUE);
  int offset = 5;
  EncoderResult<ByteBuffer> result = encoder.encode(int16, offset);
  int expectedNumOfBytes = 3;
  assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x00, (byte) 0x80, 0x00};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  } else {
      byte[] expectedBytes = {0x00, 0x00, (byte) 0x80};
      byte[] actualBytes = new byte[expectedNumOfBytes];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }
  }
}
