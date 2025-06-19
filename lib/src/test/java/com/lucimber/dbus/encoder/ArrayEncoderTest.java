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
    Signature signature = Signature.valueOf("an");
    DBusArray<Int16> array = new DBusArray<>(signature);
    Encoder<DBusArray<Int16>, ByteBuffer> encoder =
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
    Signature signature = Signature.valueOf("ab");
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
    Signature signature = Signature.valueOf("ax");
    DBusArray<Int64> array = new DBusArray<>(signature);
    Encoder<DBusArray<Int64>, ByteBuffer> encoder =
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
    Signature signature = Signature.valueOf("ab");
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
    Signature signature = Signature.valueOf("ay");
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
    Signature signature = Signature.valueOf("ad");
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
    Signature signature = Signature.valueOf("ai");
    DBusArray<Int32> array = new DBusArray<>(signature);
    array.add(Int32.valueOf(Integer.MIN_VALUE));
    array.add(Int32.valueOf(Integer.MAX_VALUE));
    array.add(Int32.valueOf(Integer.MIN_VALUE));
    array.add(Int32.valueOf(Integer.MAX_VALUE));
    Encoder<DBusArray<Int32>, ByteBuffer> encoder =
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
    Signature signature = Signature.valueOf("at");
    DBusArray<UInt64> array = new DBusArray<>(signature);
    array.add(UInt64.valueOf(Long.MIN_VALUE));
    array.add(UInt64.valueOf(Long.MAX_VALUE));
    Encoder<DBusArray<UInt64>, ByteBuffer> encoder =
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
    Signature signature = Signature.valueOf("ao");
    DBusArray<ObjectPath> array = new DBusArray<>(signature);
    array.add(ObjectPath.valueOf("/abc1")); // 4 SIZE + 5 CHARS + 1 NUL + 2 PADDING = 12
    array.add(ObjectPath.valueOf("/def23")); // 4 SIZE + 6 CHARS + 1 NUL + 1 PADDING = 12
    array.add(ObjectPath.valueOf("/_9h")); // 4 SIZE + 4 CHARS + 1 NUL + 0 PADDING = 9
    Encoder<DBusArray<ObjectPath>, ByteBuffer> encoder = new ArrayEncoder<>(byteOrder, signature);
    EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
    int numOfBytes = 37;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeShortArray(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("aq");
    DBusArray<UInt16> array = new DBusArray<>(signature);
    array.add(UInt16.valueOf(Short.MIN_VALUE));
    array.add(UInt16.valueOf(Short.MAX_VALUE));
    Encoder<DBusArray<UInt16>, ByteBuffer> encoder = new ArrayEncoder<>(byteOrder, signature);
    EncoderResult<ByteBuffer> result = encoder.encode(array, 0);
    int numOfBytes = 8;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSignatureArray(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("ag");
    DBusArray<Signature> array = new DBusArray<>(signature);
    array.add(Signature.valueOf("(id)")); // 1 SIZE + 4 CHARS + 1 NUL = 6
    array.add(Signature.valueOf("ayv")); // 1 SIZE + 3 CHARS + 1 NUL = 5
    array.add(Signature.valueOf("ba{yi}v")); // 1 SIZE + 7 CHARS + 1 NUL = 9
    Encoder<DBusArray<Signature>, ByteBuffer> encoder =
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
    Signature signature = Signature.valueOf("as");
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
}
