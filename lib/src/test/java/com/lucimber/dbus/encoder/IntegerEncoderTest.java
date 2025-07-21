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

  @Test
  void encodeSignedIntegerWithMultipleOffsets() {
  DBusInt32 int32 = DBusInt32.valueOf(0x12345678);
  
  // Test Big Endian with multiple offsets
  Encoder<DBusInt32, ByteBuffer> bigEndianEncoder = new Int32Encoder(ByteOrder.BIG_ENDIAN);
  
  // Offset 0 - no padding needed
  EncoderResult<ByteBuffer> result0 = bigEndianEncoder.encode(int32, 0);
  assertEquals(4, result0.getProducedBytes(), "Offset 0: " + PRODUCED_BYTES);
  byte[] expected0 = {0x12, 0x34, 0x56, 0x78};
  byte[] actual0 = new byte[4];
  result0.getBuffer().get(0, actual0);
  assertArrayEquals(expected0, actual0, "Big Endian Offset 0");
  
  // Offset 1 - requires 3 bytes padding to reach 4-byte boundary
  EncoderResult<ByteBuffer> result1 = bigEndianEncoder.encode(int32, 1);
  assertEquals(7, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);
  byte[] expected1 = {0x00, 0x00, 0x00, 0x12, 0x34, 0x56, 0x78};
  byte[] actual1 = new byte[7];
  result1.getBuffer().get(0, actual1);
  assertArrayEquals(expected1, actual1, "Big Endian Offset 1");
  
  // Offset 2 - requires 2 bytes padding to reach 4-byte boundary
  EncoderResult<ByteBuffer> result2 = bigEndianEncoder.encode(int32, 2);
  assertEquals(6, result2.getProducedBytes(), "Offset 2: " + PRODUCED_BYTES);
  byte[] expected2 = {0x00, 0x00, 0x12, 0x34, 0x56, 0x78};
  byte[] actual2 = new byte[6];
  result2.getBuffer().get(0, actual2);
  assertArrayEquals(expected2, actual2, "Big Endian Offset 2");
  
  // Offset 3 - requires 1 byte padding to reach 4-byte boundary
  EncoderResult<ByteBuffer> result3 = bigEndianEncoder.encode(int32, 3);
  assertEquals(5, result3.getProducedBytes(), "Offset 3: " + PRODUCED_BYTES);
  byte[] expected3 = {0x00, 0x12, 0x34, 0x56, 0x78};
  byte[] actual3 = new byte[5];
  result3.getBuffer().get(0, actual3);
  assertArrayEquals(expected3, actual3, "Big Endian Offset 3");
  
  // Offset 4 - no padding needed, already at 4-byte boundary
  EncoderResult<ByteBuffer> result4 = bigEndianEncoder.encode(int32, 4);
  assertEquals(4, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);
  byte[] expected4 = {0x12, 0x34, 0x56, 0x78};
  byte[] actual4 = new byte[4];
  result4.getBuffer().get(0, actual4);
  assertArrayEquals(expected4, actual4, "Big Endian Offset 4");
  }

  @Test
  void encodeSignedIntegerWithMultipleOffsetsLittleEndian() {
  DBusInt32 int32 = DBusInt32.valueOf(0x12345678);
  
  // Test Little Endian with multiple offsets
  Encoder<DBusInt32, ByteBuffer> littleEndianEncoder = new Int32Encoder(ByteOrder.LITTLE_ENDIAN);
  
  // Offset 0 - no padding needed
  EncoderResult<ByteBuffer> result0 = littleEndianEncoder.encode(int32, 0);
  assertEquals(4, result0.getProducedBytes(), "Offset 0: " + PRODUCED_BYTES);
  byte[] expected0 = {0x78, 0x56, 0x34, 0x12};
  byte[] actual0 = new byte[4];
  result0.getBuffer().get(0, actual0);
  assertArrayEquals(expected0, actual0, "Little Endian Offset 0");
  
  // Offset 1 - requires 3 bytes padding to reach 4-byte boundary
  EncoderResult<ByteBuffer> result1 = littleEndianEncoder.encode(int32, 1);
  assertEquals(7, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);
  byte[] expected1 = {0x00, 0x00, 0x00, 0x78, 0x56, 0x34, 0x12};
  byte[] actual1 = new byte[7];
  result1.getBuffer().get(0, actual1);
  assertArrayEquals(expected1, actual1, "Little Endian Offset 1");
  
  // Offset 2 - requires 2 bytes padding to reach 4-byte boundary
  EncoderResult<ByteBuffer> result2 = littleEndianEncoder.encode(int32, 2);
  assertEquals(6, result2.getProducedBytes(), "Offset 2: " + PRODUCED_BYTES);
  byte[] expected2 = {0x00, 0x00, 0x78, 0x56, 0x34, 0x12};
  byte[] actual2 = new byte[6];
  result2.getBuffer().get(0, actual2);
  assertArrayEquals(expected2, actual2, "Little Endian Offset 2");
  
  // Offset 3 - requires 1 byte padding to reach 4-byte boundary
  EncoderResult<ByteBuffer> result3 = littleEndianEncoder.encode(int32, 3);
  assertEquals(5, result3.getProducedBytes(), "Offset 3: " + PRODUCED_BYTES);
  byte[] expected3 = {0x00, 0x78, 0x56, 0x34, 0x12};
  byte[] actual3 = new byte[5];
  result3.getBuffer().get(0, actual3);
  assertArrayEquals(expected3, actual3, "Little Endian Offset 3");
  
  // Offset 4 - no padding needed, already at 4-byte boundary
  EncoderResult<ByteBuffer> result4 = littleEndianEncoder.encode(int32, 4);
  assertEquals(4, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);
  byte[] expected4 = {0x78, 0x56, 0x34, 0x12};
  byte[] actual4 = new byte[4];
  result4.getBuffer().get(0, actual4);
  assertArrayEquals(expected4, actual4, "Little Endian Offset 4");
  }

  @Test
  void encodeSignedIntegerBoundaryValues() {
  Encoder<DBusInt32, ByteBuffer> encoder = new Int32Encoder(ByteOrder.BIG_ENDIAN);
  
  // Test with maximum value
  DBusInt32 maxValue = DBusInt32.valueOf(Integer.MAX_VALUE);
  EncoderResult<ByteBuffer> maxResult = encoder.encode(maxValue, 0);
  assertEquals(4, maxResult.getProducedBytes(), "Max value: " + PRODUCED_BYTES);
  byte[] expectedMax = {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
  byte[] actualMax = new byte[4];
  maxResult.getBuffer().get(0, actualMax);
  assertArrayEquals(expectedMax, actualMax, "Max value bytes");
  
  // Test with zero
  DBusInt32 zeroValue = DBusInt32.valueOf(0);
  EncoderResult<ByteBuffer> zeroResult = encoder.encode(zeroValue, 0);
  assertEquals(4, zeroResult.getProducedBytes(), "Zero value: " + PRODUCED_BYTES);
  byte[] expectedZero = {0x00, 0x00, 0x00, 0x00};
  byte[] actualZero = new byte[4];
  zeroResult.getBuffer().get(0, actualZero);
  assertArrayEquals(expectedZero, actualZero, "Zero value bytes");
  
  // Test with -1
  DBusInt32 minusOne = DBusInt32.valueOf(-1);
  EncoderResult<ByteBuffer> minusOneResult = encoder.encode(minusOne, 0);
  assertEquals(4, minusOneResult.getProducedBytes(), "Minus one: " + PRODUCED_BYTES);
  byte[] expectedMinusOne = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
  byte[] actualMinusOne = new byte[4];
  minusOneResult.getBuffer().get(0, actualMinusOne);
  assertArrayEquals(expectedMinusOne, actualMinusOne, "Minus one bytes");
  }
}
