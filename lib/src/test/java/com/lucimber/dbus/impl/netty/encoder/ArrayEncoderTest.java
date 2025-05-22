/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Int16;
import com.lucimber.dbus.type.Int32;
import com.lucimber.dbus.type.Int64;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt16;
import com.lucimber.dbus.type.UInt64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class ArrayEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeEmptyShortArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("an");
    final DBusArray<Int16> array = new DBusArray<>(signature);
    final Encoder<DBusArray<Int16>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 4;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeEmptyBooleanArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ab");
    final DBusArray<DBusBoolean> array = new DBusArray<>(signature);
    final Encoder<DBusArray<DBusBoolean>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 4;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeEmptyLongArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ax");
    final DBusArray<Int64> array = new DBusArray<>(signature);
    final Encoder<DBusArray<Int64>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int expectedBytes = 8;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeBooleanArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ab");
    final DBusArray<DBusBoolean> array = new DBusArray<>(signature);
    array.add(DBusBoolean.valueOf(true));
    array.add(DBusBoolean.valueOf(false));
    array.add(DBusBoolean.valueOf(true));
    final Encoder<DBusArray<DBusBoolean>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 16;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeByteArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ay");
    final DBusArray<DBusByte> array = new DBusArray<>(signature);
    final byte one = 0x01;
    array.add(DBusByte.valueOf(one));
    final byte two = 0x02;
    array.add(DBusByte.valueOf(two));
    final byte three = 0x03;
    array.add(DBusByte.valueOf(three));
    final byte four = 0x04;
    array.add(DBusByte.valueOf(four));
    final byte five = 0x05;
    array.add(DBusByte.valueOf(five));
    final Encoder<DBusArray<DBusByte>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 9;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeDoubleArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ad");
    final DBusArray<DBusDouble> array = new DBusArray<>(signature);
    array.add(DBusDouble.valueOf(Double.MIN_VALUE));
    array.add(DBusDouble.valueOf(Double.MAX_VALUE));
    array.add(DBusDouble.valueOf(Double.MIN_VALUE));
    array.add(DBusDouble.valueOf(Double.MAX_VALUE));
    final Encoder<DBusArray<DBusDouble>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 40;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeIntegerArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ai");
    final DBusArray<Int32> array = new DBusArray<>(signature);
    array.add(Int32.valueOf(Integer.MIN_VALUE));
    array.add(Int32.valueOf(Integer.MAX_VALUE));
    array.add(Int32.valueOf(Integer.MIN_VALUE));
    array.add(Int32.valueOf(Integer.MAX_VALUE));
    final Encoder<DBusArray<Int32>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 20;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeLongArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("at");
    final DBusArray<UInt64> array = new DBusArray<>(signature);
    array.add(UInt64.valueOf(Long.MIN_VALUE));
    array.add(UInt64.valueOf(Long.MAX_VALUE));
    final Encoder<DBusArray<UInt64>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 24;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeObjectPathArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ao");
    final DBusArray<ObjectPath> array = new DBusArray<>(signature);
    array.add(ObjectPath.valueOf("/abc1")); // 4 SIZE + 5 CHARS + 1 NUL + 2 PADDING = 12
    array.add(ObjectPath.valueOf("/def23")); // 4 SIZE + 6 CHARS + 1 NUL + 1 PADDING = 12
    array.add(ObjectPath.valueOf("/_9h")); // 4 SIZE + 4 CHARS + 1 NUL + 0 PADDING = 9
    final Encoder<DBusArray<ObjectPath>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 37;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeShortArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("aq");
    final DBusArray<UInt16> array = new DBusArray<>(signature);
    array.add(UInt16.valueOf(Short.MIN_VALUE));
    array.add(UInt16.valueOf(Short.MAX_VALUE));
    final Encoder<DBusArray<UInt16>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 8;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeSignatureArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("ag");
    final DBusArray<Signature> array = new DBusArray<>(signature);
    array.add(Signature.valueOf("(id)")); // 1 SIZE + 4 CHARS + 1 NUL = 6
    array.add(Signature.valueOf("ayv")); // 1 SIZE + 3 CHARS + 1 NUL = 5
    array.add(Signature.valueOf("ba{yi}v")); // 1 SIZE + 7 CHARS + 1 NUL = 9
    final Encoder<DBusArray<Signature>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 24;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeStringArray(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("as");
    final DBusArray<DBusString> array = new DBusArray<>(signature);
    array.add(DBusString.valueOf("abc1")); // 4 SIZE + 4 CHARS + 1 NUL + 3 PADDING = 12
    array.add(DBusString.valueOf("def23")); // 4 SIZE + 5 CHARS + 1 NUL + 2 PADDING = 12
    array.add(DBusString.valueOf("_9h")); // 4 SIZE + 3 CHARS + 1 NUL + 0 PADDING = 8
    final Encoder<DBusArray<DBusString>, ByteBuf> encoder =
            new ArrayEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(array, 0);
    final int numOfBytes = 36;
    assertEquals(numOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(numOfBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }
}
