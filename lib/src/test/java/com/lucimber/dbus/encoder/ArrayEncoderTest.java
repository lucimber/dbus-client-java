/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ArrayEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptyShortArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("an");
  DBusArray<DBusInt16> array = new DBusArray<>(signature);
  Encoder<DBusArray<DBusInt16>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 4;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptyBooleanArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ab");
  DBusArray<DBusBoolean> array = new DBusArray<>(signature);
  Encoder<DBusArray<DBusBoolean>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 4;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptyLongArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ax");
  DBusArray<DBusInt64> array = new DBusArray<>(signature);
  Encoder<DBusArray<DBusInt64>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int expectedBytes = 8;
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ab");
  DBusArray<DBusBoolean> array = new DBusArray<>(signature);
  array.add(DBusBoolean.valueOf(true));
  array.add(DBusBoolean.valueOf(false));
  array.add(DBusBoolean.valueOf(true));
  Encoder<DBusArray<DBusBoolean>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 16;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeByteArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ay");
  DBusArray<DBusByte> array = new DBusArray<>(signature);
  byte one = 0x01;
  array.add(DBusByte.valueOf(one));
  byte two = 0x02;
  array.add(DBusByte.valueOf(two));
  byte three = 0x03;
  array.add(DBusByte.valueOf(three));
  byte four = 0x04;
  array.add(DBusByte.valueOf(four));
  byte five = 0x05;
  array.add(DBusByte.valueOf(five));
  Encoder<DBusArray<DBusByte>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 9;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeDoubleArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ad");
  DBusArray<DBusDouble> array = new DBusArray<>(signature);
  array.add(DBusDouble.valueOf(Double.MIN_VALUE));
  array.add(DBusDouble.valueOf(Double.MAX_VALUE));
  array.add(DBusDouble.valueOf(Double.MIN_VALUE));
  array.add(DBusDouble.valueOf(Double.MAX_VALUE));
  Encoder<DBusArray<DBusDouble>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 40;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeIntegerArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ai");
  DBusArray<DBusInt32> array = new DBusArray<>(signature);
  array.add(DBusInt32.valueOf(Integer.MIN_VALUE));
  array.add(DBusInt32.valueOf(Integer.MAX_VALUE));
  array.add(DBusInt32.valueOf(Integer.MIN_VALUE));
  array.add(DBusInt32.valueOf(Integer.MAX_VALUE));
  Encoder<DBusArray<DBusInt32>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 20;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeLongArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("at");
  DBusArray<DBusUInt64> array = new DBusArray<>(signature);
  array.add(DBusUInt64.valueOf(Long.MIN_VALUE));
  array.add(DBusUInt64.valueOf(Long.MAX_VALUE));
  Encoder<DBusArray<DBusUInt64>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 24;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeObjectPathArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ao");
  DBusArray<DBusObjectPath> array = new DBusArray<>(signature);
  array.add(DBusObjectPath.valueOf("/abc1")); // 4 SIZE + 5 CHARS + 1 NUL + 2 PADDING = 12
  array.add(DBusObjectPath.valueOf("/def23")); // 4 SIZE + 6 CHARS + 1 NUL + 1 PADDING = 12
  array.add(DBusObjectPath.valueOf("/_9h")); // 4 SIZE + 4 CHARS + 1 NUL + 0 PADDING = 9
  Encoder<DBusArray<DBusObjectPath>, ByteBuffer> encoder = new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 37;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeShortArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("aq");
  DBusArray<DBusUInt16> array = new DBusArray<>(signature);
  array.add(DBusUInt16.valueOf(Short.MIN_VALUE));
  array.add(DBusUInt16.valueOf(Short.MAX_VALUE));
  Encoder<DBusArray<DBusUInt16>, ByteBuffer> encoder = new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 8;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSignatureArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ag");
  DBusArray<DBusSignature> array = new DBusArray<>(signature);
  array.add(DBusSignature.valueOf("(id)")); // 1 SIZE + 4 CHARS + 1 NUL = 6
  array.add(DBusSignature.valueOf("ayv")); // 1 SIZE + 3 CHARS + 1 NUL = 5
  array.add(DBusSignature.valueOf("ba{yi}v")); // 1 SIZE + 7 CHARS + 1 NUL = 9
  Encoder<DBusArray<DBusSignature>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 24;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeStringArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("as");
  DBusArray<DBusString> array = new DBusArray<>(signature);
  array.add(DBusString.valueOf("abc1")); // 4 SIZE + 4 CHARS + 1 NUL + 3 PADDING = 12
  array.add(DBusString.valueOf("def23")); // 4 SIZE + 5 CHARS + 1 NUL + 2 PADDING = 12
  array.add(DBusString.valueOf("_9h")); // 4 SIZE + 3 CHARS + 1 NUL + 0 PADDING = 8
  Encoder<DBusArray<DBusString>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
  int numOfBytes = 36;
  assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeIntegerArrayWithMultipleOffsets(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ai");
  DBusArray<DBusInt32> array = new DBusArray<>(signature);
  array.add(DBusInt32.valueOf(1));
  array.add(DBusInt32.valueOf(2));
  array.add(DBusInt32.valueOf(3));
  Encoder<DBusArray<DBusInt32>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  
  // Test with offset 0 (no padding needed)
  EncoderResult<ByteBuffer> result0 = encoder.encode(array, 0);
  int expectedBytes0 = 16; // 4 array length + 3*4 element bytes
  assertEquals(expectedBytes0, result0.getProducedBytes(), "Offset 0: " + PRODUCED_BYTES);
  
  // Test with offset 1 (requires 3 bytes padding to reach 4-byte boundary)
  EncoderResult<ByteBuffer> result1 = encoder.encode(array, 1);
  int expectedBytes1 = 19; // 3 padding + 4 array length + 3*4 element bytes
  assertEquals(expectedBytes1, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);
  
  // Test with offset 2 (requires 2 bytes padding to reach 4-byte boundary)
  EncoderResult<ByteBuffer> result2 = encoder.encode(array, 2);
  int expectedBytes2 = 18; // 2 padding + 4 array length + 3*4 element bytes
  assertEquals(expectedBytes2, result2.getProducedBytes(), "Offset 2: " + PRODUCED_BYTES);
  
  // Test with offset 3 (requires 1 byte padding to reach 4-byte boundary)
  EncoderResult<ByteBuffer> result3 = encoder.encode(array, 3);
  int expectedBytes3 = 17; // 1 padding + 4 array length + 3*4 element bytes
  assertEquals(expectedBytes3, result3.getProducedBytes(), "Offset 3: " + PRODUCED_BYTES);
  
  // Test with offset 4 (no padding needed, already at 4-byte boundary)
  EncoderResult<ByteBuffer> result4 = encoder.encode(array, 4);
  int expectedBytes4 = 16; // 0 padding + 4 array length + 3*4 element bytes
  assertEquals(expectedBytes4, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeDoubleArrayWithMultipleOffsets(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ad");
  DBusArray<DBusDouble> array = new DBusArray<>(signature);
  array.add(DBusDouble.valueOf(1.5));
  array.add(DBusDouble.valueOf(2.5));
  Encoder<DBusArray<DBusDouble>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  
  // Test with various offsets - doubles require 8-byte alignment
  for (int offset = 0; offset < 16; offset++) {
      EncoderResult<ByteBuffer> result = encoder.encode(array, offset);
      
      // Calculate expected padding to reach 8-byte boundary for array content
      int paddingToArrayLength = (4 - (offset % 4)) % 4;
      int arrayContentOffset = offset + paddingToArrayLength + 4; // after padding + array length
      int paddingToDoubleAlignment = (8 - (arrayContentOffset % 8)) % 8;
      
      int expectedBytes = paddingToArrayLength + 4 + paddingToDoubleAlignment + 2 * 8;
      
      assertEquals(expectedBytes, result.getProducedBytes(), 
                   "Offset " + offset + ": " + PRODUCED_BYTES);
      
      ByteBuffer buffer = result.getBuffer();
      assertEquals(expectedBytes, buffer.remaining(), 
                   "Offset " + offset + ": " + READABLE_BYTES);
  }
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptyArrayWithMultipleOffsets(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("ai");
  DBusArray<DBusInt32> array = new DBusArray<>(signature);
  Encoder<DBusArray<DBusInt32>, ByteBuffer> encoder =
          new ArrayEncoder<>(byteOrder, signature);
  
  // Test with various offsets for empty array
  for (int offset = 0; offset < 8; offset++) {
      EncoderResult<ByteBuffer> result = encoder.encode(array, offset);
      
      // Calculate expected padding to reach 4-byte boundary
      int paddingNeeded = (4 - (offset % 4)) % 4;
      int expectedBytes = paddingNeeded + 4; // padding + array length (0)
      
      assertEquals(expectedBytes, result.getProducedBytes(), 
                   "Offset " + offset + ": " + PRODUCED_BYTES);
      
      ByteBuffer buffer = result.getBuffer();
      assertEquals(expectedBytes, buffer.remaining(), 
                   "Offset " + offset + ": " + READABLE_BYTES);
  }
  }
}
