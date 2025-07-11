/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusInt64;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class LongEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @Test
  void encodeSignedLongMinValueOnBigEndian() {
    Encoder<DBusInt64, ByteBuffer> encoder = new Int64Encoder(ByteOrder.BIG_ENDIAN);
    DBusInt64 int64 = DBusInt64.valueOf(-9223372036854775808L);
    EncoderResult<ByteBuffer> result = encoder.encode(int64, 0);
    int expectedNumOfBytes = 8;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  }

  @Test
  void encodeSignedLongMinValueWithOffsetOnBigEndian() {
    Encoder<DBusInt64, ByteBuffer> encoder = new Int64Encoder(ByteOrder.BIG_ENDIAN);
    DBusInt64 int64 = DBusInt64.valueOf(-9223372036854775808L);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(int64, offset);
    int expectedNumOfBytes = 11;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  }

  @Test
  void encodeSignedLongMinValueOnLittleEndian() {
    Encoder<DBusInt64, ByteBuffer> encoder = new Int64Encoder(ByteOrder.LITTLE_ENDIAN);
    DBusInt64 int64 = DBusInt64.valueOf(-9223372036854775808L);
    EncoderResult<ByteBuffer> result = encoder.encode(int64, 0);
    int expectedNumOfBytes = 8;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }

  @Test
  void encodeSignedLongMinValueWithOffsetOnLittleEndian() {
    Encoder<DBusInt64, ByteBuffer> encoder = new Int64Encoder(ByteOrder.LITTLE_ENDIAN);
    DBusInt64 int64 = DBusInt64.valueOf(-9223372036854775808L);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(int64, offset);
    int expectedNumOfBytes = 11;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }
}
