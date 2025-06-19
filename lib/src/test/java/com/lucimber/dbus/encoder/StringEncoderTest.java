/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class StringEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSimpleString(ByteOrder byteOrder) {
    Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
    DBusString dbusString = DBusString.valueOf("abcABC_äüö");
    EncoderResult<ByteBuffer> result = encoder.encode(dbusString, 0);
    int expectedNumOfBytes = 18;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    // UINT32 bytes
    int numOfLengthBytes = 4;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0D};
      byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.get(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      byte[] expectedBytes = {0x0D, 0x00, 0x00, 0x00};
      byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.get(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    // UINT32 bytes (4) + UTF-8 bytes (13)
    int numOfRemainingBytes = 17;
    buffer.position(numOfRemainingBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSimpleStringWithOffset(ByteOrder byteOrder) {
    Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
    DBusString dbusString = DBusString.valueOf("abcABC_äüö");
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(dbusString, offset);
    int expectedNumOfBytes = 21;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    int numOfOffsetBytes = 3;
    buffer.position(numOfOffsetBytes);
    // UINT32 bytes
    int numOfLengthBytes = 4;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0D};
      byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.get(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      byte[] expectedBytes = {0x0D, 0x00, 0x00, 0x00};
      byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.get(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    // 3 + UINT32 bytes (4) + UTF-8 bytes (13)
    int numOfRemainingBytes = 20;
    buffer.position(numOfRemainingBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptyString(ByteOrder byteOrder) {
    Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
    DBusString dbusString = DBusString.valueOf("");
    EncoderResult<ByteBuffer> result = encoder.encode(dbusString, 0);
    int expectedNumOfBytes = 5;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    // UINT32
    int numOfLengthBytes = 4;
    byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[numOfLengthBytes];
    buffer.get(actualBytes, 0, numOfLengthBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeStringOfObjectPath(ByteOrder byteOrder) {
    ObjectPath objectPath = ObjectPath.valueOf("/abc/d1/e_f");
    DBusString rawPath = DBusString.valueOf(objectPath.getWrappedValue().toString());
    Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
    EncoderResult<ByteBuffer> result = encoder.encode(rawPath, 0);
    int expectedNumOfBytes = 16;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    // UINT32 bytes
    int numOfLengthBytes = 4;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0B};
      byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.get(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      byte[] expectedBytes = {0x0B, 0x00, 0x00, 0x00};
      byte[] actualBytes = new byte[numOfLengthBytes];
      buffer.get(actualBytes, 0, numOfLengthBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    // UINT32 bytes (4) + UTF-8 bytes (11 bytes)
    int numOfRemainingBytes = 15;
    buffer.position(numOfRemainingBytes);
    // Trailing NUL byte
    assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
  }
}
