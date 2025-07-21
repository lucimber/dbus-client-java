/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArrayDecoderTest {

  private static final String ARRAY_OF_BYTES = "ay";
  private static final String ARRAY_OF_BYTE_ARRAYS = "aay";
  private static final String ARRAY_OF_DOUBLES = "ad";
  private static final String ARRAY_OF_DOUBLE_ARRAYS = "aad";
  private static final String ARRAY_OF_SIGNED_LONGS = "ax";
  private static final String ARRAY_OF_SIGNED_SHORTS = "an";
  private static final String ARRAY_OF_STRINGS = "as";
  private static final String HEADER_SIGNATURE = "a(yv)";
  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
  private static final String ASSERT_SIZE_OF_ARRAY = "Size of array";

  private static ByteBuffer allocateAligned(int size, ByteOrder order) {
  return ByteBuffer.allocate(size).order(order);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfBytes(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ARRAY_OF_BYTES);
  byte[] items = {0x01, 0x02, 0x03, 0x04, 0x05};
  ByteBuffer buffer = allocateAligned(4 + items.length, byteOrder);
  buffer.putInt(items.length);
  buffer.put(items);
  buffer.flip();
  ArrayDecoder<DBusByte> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusByte>> result = decoder.decode(buffer, 0);
  assertEquals(buffer.limit(), result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(items.length, result.getValue().size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeMessageHeader(ByteOrder byteOrder) {
  ByteBuffer buffer = allocateAligned(56, byteOrder);
  buffer.putInt(52);
  buffer.put(new byte[4]); // struct padding

  // Struct 1 (y -> length=4, v -> "s" + "string")
  buffer.put((byte) 4); // y
  buffer.put((byte) 1); // sig length
  buffer.put((byte) 's');
  buffer.put((byte) 0);
  byte[] s1 = "io.lucimber.Error.UnitTest".getBytes(StandardCharsets.UTF_8);
  buffer.putInt(s1.length);
  buffer.put(s1);
  buffer.put((byte) 0);

  buffer.put(new byte[5]); // padding

  // Struct 2 (y -> length=5, v -> "i" + int32)
  buffer.put((byte) 5);
  buffer.put((byte) 1);
  buffer.put((byte) 'i');
  buffer.put((byte) 0);
  buffer.putInt(7);

  buffer.flip();
  DBusSignature signature = DBusSignature.valueOf(HEADER_SIGNATURE);
  ArrayDecoder<DBusStruct> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusStruct>> result = decoder.decode(buffer, 0);
  assertEquals(56, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(2, result.getValue().size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfArrays(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ARRAY_OF_BYTE_ARRAYS);
  ByteBuffer buffer = allocateAligned(26, byteOrder);
  buffer.putInt(22);
  for (int i = 0; i < 3; i++) {
      buffer.putInt(2);
      buffer.put((byte) 0x01).put((byte) 0x02);
      if (i < 2) buffer.put(new byte[2]);
  }
  buffer.flip();
  ArrayDecoder<DBusArray<DBusByte>> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusArray<DBusByte>>> result = decoder.decode(buffer, 0);
  assertEquals(26, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(3, result.getValue().size(), ASSERT_SIZE_OF_ARRAY);
  assertEquals(2, result.getValue().get(0).size());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfDoubles(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ARRAY_OF_DOUBLES);
  ByteBuffer buffer = allocateAligned(32, byteOrder);
  buffer.putInt(24);
  buffer.putInt(0);
  buffer.putDouble(1.0);
  buffer.putDouble(2.0);
  buffer.putDouble(3.0);
  buffer.flip();
  ArrayDecoder<DBusDouble> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusDouble>> result = decoder.decode(buffer, 0);
  assertEquals(32, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(3, result.getValue().size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfDoubleArrays(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ARRAY_OF_DOUBLE_ARRAYS);
  ByteBuffer buffer = allocateAligned(96, byteOrder);
  buffer.putInt(92);
  for (int i = 0; i < 3; i++) {
      buffer.putInt(24);
      if (i > 0) buffer.put(new byte[4]);
      buffer.putDouble(1.0);
      buffer.putDouble(2.0);
      buffer.putDouble(3.0);
  }
  buffer.flip();
  ArrayDecoder<DBusArray<DBusDouble>> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusArray<DBusDouble>>> result = decoder.decode(buffer, 0);
  assertEquals(96, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(3, result.getValue().size(), ASSERT_SIZE_OF_ARRAY);
  assertEquals(3, result.getValue().get(1).size());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfSignedShorts(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ARRAY_OF_SIGNED_SHORTS);
  ByteBuffer buffer = allocateAligned(12, byteOrder);

  buffer.putInt(8);
  for (short s = 1; s <= 4; s++) {
      buffer.putShort(s);
  }
  buffer.flip();

  ArrayDecoder<DBusInt16> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusInt16>> result = decoder.decode(buffer, 0);

  assertEquals(12, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(4, result.getValue().size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfStrings(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ARRAY_OF_STRINGS);
  String[] strings = {"abc1", "def2"};
  ByteBuffer content = allocateAligned(100, byteOrder);

  for (int i = 0; i < strings.length; i++) {
      byte[] bytes = strings[i].getBytes(StandardCharsets.UTF_8);
      content.putInt(bytes.length);
      content.put(bytes);
      content.put((byte) 0);
      if (i == 0) content.put(new byte[3]);
  }
  content.flip();

  ByteBuffer buffer = allocateAligned(4 + content.limit(), byteOrder);
  buffer.putInt(content.limit());
  buffer.put(content);
  buffer.flip();

  ArrayDecoder<DBusString> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusString>> result = decoder.decode(buffer, 0);
  assertEquals(buffer.limit(), result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(2, result.getValue().size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeEmptyArrayOfSignedLongs(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ARRAY_OF_SIGNED_LONGS);
  ByteBuffer buffer = allocateAligned(8, byteOrder);

  buffer.putInt(0);
  buffer.putInt(0); // padding
  buffer.flip();

  ArrayDecoder<DBusInt64> decoder = new ArrayDecoder<>(signature);
  DecoderResult<DBusArray<DBusInt64>> result = decoder.decode(buffer, 0);
  assertEquals(8, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertTrue(result.getValue().isEmpty(), ASSERT_SIZE_OF_ARRAY);
  }
}
