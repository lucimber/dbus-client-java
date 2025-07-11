/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusInt32;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class IntegerEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @Test
  void encodeSignedIntegerMinValueOnBigEndian() {
    Encoder<DBusInt32, ByteBuffer> encoder = new Int32Encoder(ByteOrder.BIG_ENDIAN);
    DBusInt32 int32 = DBusInt32.valueOf(-2147483648);
    EncoderResult<ByteBuffer> result = encoder.encode(int32, 0);
    int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {(byte) 0x80, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  }

  @Test
  void encodeSignedIntegerMinValueWithOffsetOnBigEndian() {
    Encoder<DBusInt32, ByteBuffer> encoder = new Int32Encoder(ByteOrder.BIG_ENDIAN);
    DBusInt32 int32 = DBusInt32.valueOf(-2147483648);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(int32, offset);
    int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
  }

  @Test
  void encodeSignedIntegerMinValueOnLittleEndian() {
    Encoder<DBusInt32, ByteBuffer> encoder = new Int32Encoder(ByteOrder.LITTLE_ENDIAN);
    DBusInt32 int32 = DBusInt32.valueOf(-2147483648);
    EncoderResult<ByteBuffer> result = encoder.encode(int32, 0);
    int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, (byte) 0x80};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }

  @Test
  void encodeSignedIntegerMinValueWithOffsetOnLittleEndian() {
    Encoder<DBusInt32, ByteBuffer> encoder = new Int32Encoder(ByteOrder.LITTLE_ENDIAN);
    DBusInt32 int32 = DBusInt32.valueOf(-2147483648);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(int32, offset);
    int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
  }
}
