/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusUInt16;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class UInt16EncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedShortMaxValue(ByteOrder byteOrder) {
  Encoder<DBusUInt16, ByteBuffer> encoder = new UInt16Encoder(byteOrder);
  DBusUInt16 uint16 = DBusUInt16.valueOf((short) -1); // 0xFFFF as unsigned
  EncoderResult<ByteBuffer> result = encoder.encode(uint16, 0);
  int expectedNumOfBytes = 2;
  assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
  
  byte[] expectedBytes;
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expectedBytes = new byte[]{(byte) 0xFF, (byte) 0xFF};
  } else {
      expectedBytes = new byte[]{(byte) 0xFF, (byte) 0xFF};
  }
  byte[] actualBytes = new byte[expectedNumOfBytes];
  buffer.get(0, actualBytes);
  assertArrayEquals(expectedBytes, actualBytes, byteOrder + " byte order");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedShortMinValue(ByteOrder byteOrder) {
  Encoder<DBusUInt16, ByteBuffer> encoder = new UInt16Encoder(byteOrder);
  DBusUInt16 uint16 = DBusUInt16.valueOf((short) 0);
  EncoderResult<ByteBuffer> result = encoder.encode(uint16, 0);
  int expectedNumOfBytes = 2;
  assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
  
  byte[] expectedBytes = {0x00, 0x00};
  byte[] actualBytes = new byte[expectedNumOfBytes];
  buffer.get(0, actualBytes);
  assertArrayEquals(expectedBytes, actualBytes, byteOrder + " byte order");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedShortWithMultipleOffsets(ByteOrder byteOrder) {
  DBusUInt16 uint16 = DBusUInt16.valueOf((short) 0x1234);
  Encoder<DBusUInt16, ByteBuffer> encoder = new UInt16Encoder(byteOrder);
  
  // Test with offset 0 - no padding needed
  EncoderResult<ByteBuffer> result0 = encoder.encode(uint16, 0);
  assertEquals(2, result0.getProducedBytes(), "Offset 0: " + PRODUCED_BYTES);
  byte[] expected0, actual0 = new byte[2];
  result0.getBuffer().get(0, actual0);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected0 = new byte[]{0x12, 0x34};
  } else {
      expected0 = new byte[]{0x34, 0x12};
  }
  assertArrayEquals(expected0, actual0, byteOrder + " Offset 0");
  
  // Test with offset 1 - requires 1 byte padding to reach 2-byte boundary
  EncoderResult<ByteBuffer> result1 = encoder.encode(uint16, 1);
  assertEquals(3, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);
  byte[] expected1, actual1 = new byte[3];
  result1.getBuffer().get(0, actual1);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected1 = new byte[]{0x00, 0x12, 0x34};
  } else {
      expected1 = new byte[]{0x00, 0x34, 0x12};
  }
  assertArrayEquals(expected1, actual1, byteOrder + " Offset 1");
  
  // Test with offset 2 - no padding needed, already at 2-byte boundary
  EncoderResult<ByteBuffer> result2 = encoder.encode(uint16, 2);
  assertEquals(2, result2.getProducedBytes(), "Offset 2: " + PRODUCED_BYTES);
  byte[] expected2, actual2 = new byte[2];
  result2.getBuffer().get(0, actual2);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected2 = new byte[]{0x12, 0x34};
  } else {
      expected2 = new byte[]{0x34, 0x12};
  }
  assertArrayEquals(expected2, actual2, byteOrder + " Offset 2");
  
  // Test with offset 3 - requires 1 byte padding to reach 2-byte boundary
  EncoderResult<ByteBuffer> result3 = encoder.encode(uint16, 3);
  assertEquals(3, result3.getProducedBytes(), "Offset 3: " + PRODUCED_BYTES);
  byte[] expected3, actual3 = new byte[3];
  result3.getBuffer().get(0, actual3);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected3 = new byte[]{0x00, 0x12, 0x34};
  } else {
      expected3 = new byte[]{0x00, 0x34, 0x12};
  }
  assertArrayEquals(expected3, actual3, byteOrder + " Offset 3");
  
  // Test with offset 4 - no padding needed, already at 2-byte boundary
  EncoderResult<ByteBuffer> result4 = encoder.encode(uint16, 4);
  assertEquals(2, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);
  byte[] expected4, actual4 = new byte[2];
  result4.getBuffer().get(0, actual4);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected4 = new byte[]{0x12, 0x34};
  } else {
      expected4 = new byte[]{0x34, 0x12};
  }
  assertArrayEquals(expected4, actual4, byteOrder + " Offset 4");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedShortBoundaryValues(ByteOrder byteOrder) {
  Encoder<DBusUInt16, ByteBuffer> encoder = new UInt16Encoder(byteOrder);
  
  // Test with value 1
  DBusUInt16 one = DBusUInt16.valueOf((short) 1);
  EncoderResult<ByteBuffer> result1 = encoder.encode(one, 0);
  assertEquals(2, result1.getProducedBytes(), "Value 1: " + PRODUCED_BYTES);
  byte[] expected1, actual1 = new byte[2];
  result1.getBuffer().get(0, actual1);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected1 = new byte[]{0x00, 0x01};
  } else {
      expected1 = new byte[]{0x01, 0x00};
  }
  assertArrayEquals(expected1, actual1, byteOrder + " Value 1");
  
  // Test with value 255
  DBusUInt16 value255 = DBusUInt16.valueOf((short) 255);
  EncoderResult<ByteBuffer> result255 = encoder.encode(value255, 0);
  assertEquals(2, result255.getProducedBytes(), "Value 255: " + PRODUCED_BYTES);
  byte[] expected255, actual255 = new byte[2];
  result255.getBuffer().get(0, actual255);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected255 = new byte[]{0x00, (byte) 0xFF};
  } else {
      expected255 = new byte[]{(byte) 0xFF, 0x00};
  }
  assertArrayEquals(expected255, actual255, byteOrder + " Value 255");
  
  // Test with value 256
  DBusUInt16 value256 = DBusUInt16.valueOf((short) 256);
  EncoderResult<ByteBuffer> result256 = encoder.encode(value256, 0);
  assertEquals(2, result256.getProducedBytes(), "Value 256: " + PRODUCED_BYTES);
  byte[] expected256, actual256 = new byte[2];
  result256.getBuffer().get(0, actual256);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected256 = new byte[]{0x01, 0x00};
  } else {
      expected256 = new byte[]{0x00, 0x01};
  }
  assertArrayEquals(expected256, actual256, byteOrder + " Value 256");
  
  // Test with value 32767 (Short.MAX_VALUE)
  DBusUInt16 value32767 = DBusUInt16.valueOf(Short.MAX_VALUE);
  EncoderResult<ByteBuffer> result32767 = encoder.encode(value32767, 0);
  assertEquals(2, result32767.getProducedBytes(), "Value 32767: " + PRODUCED_BYTES);
  byte[] expected32767, actual32767 = new byte[2];
  result32767.getBuffer().get(0, actual32767);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected32767 = new byte[]{0x7F, (byte) 0xFF};
  } else {
      expected32767 = new byte[]{(byte) 0xFF, 0x7F};
  }
  assertArrayEquals(expected32767, actual32767, byteOrder + " Value 32767");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedShortHighValues(ByteOrder byteOrder) {
  Encoder<DBusUInt16, ByteBuffer> encoder = new UInt16Encoder(byteOrder);
  
  // Test with value 32768 (appears as negative in signed short)
  DBusUInt16 value32768 = DBusUInt16.valueOf((short) 32768); // -32768 as signed = 32768 as unsigned
  EncoderResult<ByteBuffer> result32768 = encoder.encode(value32768, 0);
  assertEquals(2, result32768.getProducedBytes(), "Value 32768: " + PRODUCED_BYTES);
  byte[] expected32768, actual32768 = new byte[2];
  result32768.getBuffer().get(0, actual32768);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected32768 = new byte[]{(byte) 0x80, 0x00};
  } else {
      expected32768 = new byte[]{0x00, (byte) 0x80};
  }
  assertArrayEquals(expected32768, actual32768, byteOrder + " Value 32768");
  
  // Test with value 65534 (appears as -2 in signed short)
  DBusUInt16 value65534 = DBusUInt16.valueOf((short) 65534); // -2 as signed = 65534 as unsigned
  EncoderResult<ByteBuffer> result65534 = encoder.encode(value65534, 0);
  assertEquals(2, result65534.getProducedBytes(), "Value 65534: " + PRODUCED_BYTES);
  byte[] expected65534, actual65534 = new byte[2];
  result65534.getBuffer().get(0, actual65534);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected65534 = new byte[]{(byte) 0xFF, (byte) 0xFE};
  } else {
      expected65534 = new byte[]{(byte) 0xFE, (byte) 0xFF};
  }
  assertArrayEquals(expected65534, actual65534, byteOrder + " Value 65534");
  
  // Test with value 40000 (appears as negative in signed short)
  DBusUInt16 value40000 = DBusUInt16.valueOf((short) 40000);
  EncoderResult<ByteBuffer> result40000 = encoder.encode(value40000, 0);
  assertEquals(2, result40000.getProducedBytes(), "Value 40000: " + PRODUCED_BYTES);
  byte[] expected40000, actual40000 = new byte[2];
  result40000.getBuffer().get(0, actual40000);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected40000 = new byte[]{(byte) 0x9C, 0x40};
  } else {
      expected40000 = new byte[]{0x40, (byte) 0x9C};
  }
  assertArrayEquals(expected40000, actual40000, byteOrder + " Value 40000");
  }
}