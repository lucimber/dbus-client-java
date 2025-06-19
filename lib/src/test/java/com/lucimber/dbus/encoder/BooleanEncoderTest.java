/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class BooleanEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanFalse(ByteOrder byteOrder) {
    Encoder<DBusBoolean, ByteBuffer> encoder = new BooleanEncoder(byteOrder);
    EncoderResult<ByteBuffer> result = encoder.encode(DBusBoolean.valueOf(false), 0);
    int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[buffer.remaining()];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanFalseWithOffset(ByteOrder byteOrder) {
    Encoder<DBusBoolean, ByteBuffer> encoder = new BooleanEncoder(byteOrder);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(DBusBoolean.valueOf(false), offset);
    int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[buffer.remaining()];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanTrue(ByteOrder byteOrder) {
    Encoder<DBusBoolean, ByteBuffer> encoder = new BooleanEncoder(byteOrder);
    EncoderResult<ByteBuffer> result = encoder.encode(DBusBoolean.valueOf(true), 0);
    int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x00, 0x00, 0x00, 0x01};
      byte[] actualBytes = new byte[buffer.remaining()];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      byte[] expectedBytes = {0x01, 0x00, 0x00, 0x00};
      byte[] actualBytes = new byte[buffer.remaining()];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanTrueWithOffset(ByteOrder byteOrder) {
    Encoder<DBusBoolean, ByteBuffer> encoder = new BooleanEncoder(byteOrder);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(DBusBoolean.valueOf(true), offset);
    int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
      byte[] actualBytes = new byte[buffer.remaining()];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      byte[] expectedBytes = {0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00};
      byte[] actualBytes = new byte[buffer.remaining()];
      buffer.get(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
  }
}
