/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.decoder;

import com.lucimber.dbus.type.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
  private static final String ASSERT_SIZE_OF_ARRAY = "Size of array";
  private static final String HEADER_SIGNATURE = "a(yv)";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfBytes(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_BYTES);
    final ByteBuf buffer = Unpooled.buffer();
    final int numOfBytes = 9;
    final int numOfContentBytes = 5;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytes);
    } else {
      buffer.writeIntLE(numOfContentBytes);
    }
    final byte[] items = {0x01, 0x02, 0x03, 0x04, 0x05};
    buffer.writeBytes(items);
    final ArrayDecoder<DBusByte> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<DBusByte>> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<DBusByte> array = result.getValue();
    assertEquals(items.length, array.size(), ASSERT_SIZE_OF_ARRAY);
    buffer.release();
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeMessageHeader(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    // Length of array
    final int numOfContentBytes = 52;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytes);
    } else {
      buffer.writeIntLE(numOfContentBytes);
    }
    final byte[] paddingStruct = {0x00, 0x00, 0x00, 0x00};
    buffer.writeBytes(paddingStruct);
    // Struct 1 (yv)
    final byte firstStructLength = 4;
    buffer.writeByte(firstStructLength);
    final byte[] v1Signature = "s".getBytes(StandardCharsets.UTF_8);
    buffer.writeByte(v1Signature.length);
    buffer.writeBytes(v1Signature);
    buffer.writeZero(1); // NUL byte
    final byte[] v1Content = "io.lucimber.Error.UnitTest".getBytes(StandardCharsets.UTF_8);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(v1Content.length);
    } else {
      buffer.writeIntLE(v1Content.length);
    }
    buffer.writeBytes(v1Content);
    buffer.writeZero(1); // NUL byte
    final int structVariantPadding = 5;
    buffer.writeZero(structVariantPadding);
    // Struct 2 (yv)
    final byte secondStructLength = 5;
    buffer.writeByte(secondStructLength);
    final byte[] v2Signature = "i".getBytes(StandardCharsets.UTF_8);
    buffer.writeByte(v2Signature.length);
    buffer.writeBytes(v2Signature);
    buffer.writeZero(1); // NUL byte
    final int randTestVal = 7;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(randTestVal);
    } else {
      buffer.writeIntLE(randTestVal);
    }
    // Test
    final int expectedBytes = 56;
    final Signature signature = Signature.valueOf(HEADER_SIGNATURE);
    final ArrayDecoder<Struct> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<Struct>> result = decoder.decode(buffer, 0);
    assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<Struct> array = result.getValue();
    final int expectedArraySize = 2;
    assertEquals(expectedArraySize, array.size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfArrays(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_BYTE_ARRAYS);
    final ByteBuf buffer = Unpooled.buffer();
    // Length of outer array
    final int numOfContentBytesOuterArray = 22;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesOuterArray);
    } else {
      buffer.writeIntLE(numOfContentBytesOuterArray);
    }
    // Arrays: aa1 + aa2 + aa3
    final int numOfContentBytesInnerArray = 2;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesInnerArray);
    } else {
      buffer.writeIntLE(numOfContentBytesInnerArray);
    }
    final byte[] innerItems = {0x01, 0x02};
    buffer.writeBytes(innerItems);
    final byte[] padding = {0x00, 0x00};
    buffer.writeBytes(padding);
    // aa2
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesInnerArray);
    } else {
      buffer.writeIntLE(numOfContentBytesInnerArray);
    }
    buffer.writeBytes(innerItems);
    buffer.writeBytes(padding);
    // aa3
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesInnerArray);
    } else {
      buffer.writeIntLE(numOfContentBytesInnerArray);
    }
    buffer.writeBytes(innerItems);
    final int expectedBytes = 26;
    final ArrayDecoder<DBusArray<DBusByte>> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<DBusArray<DBusByte>>> result = decoder.decode(buffer, 0);
    assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<DBusArray<DBusByte>> array = result.getValue();
    final int expectedArraySize = 3;
    assertEquals(expectedArraySize, array.size(), ASSERT_SIZE_OF_ARRAY);
    final int expectedInnerArraySize = 2;
    assertEquals(expectedInnerArraySize, array.get(0).size());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfDoubles(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_DOUBLES);
    final ByteBuf buffer = Unpooled.buffer();
    final int numOfContentBytes = 24;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytes);
    } else {
      buffer.writeIntLE(numOfContentBytes);
    }
    final byte[] paddingDouble = {0x00, 0x00, 0x00, 0x00};
    buffer.writeBytes(paddingDouble);
    final double one = 1;
    final double two = 2;
    final double three = 3;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeDouble(one);
      buffer.writeDouble(two);
      buffer.writeDouble(three);
    } else {
      buffer.writeDoubleLE(one);
      buffer.writeDoubleLE(two);
      buffer.writeDoubleLE(three);
    }
    final int numOfBytes = buffer.readableBytes();
    final ArrayDecoder<DBusDouble> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<DBusDouble>> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<DBusDouble> array = result.getValue();
    final int expectedArraySize = 3;
    assertEquals(expectedArraySize, array.size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfDoubleArrays(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_DOUBLE_ARRAYS);
    final ByteBuf buffer = Unpooled.buffer();
    final int numOfContentBytesOuterArray = 92;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesOuterArray);
    } else {
      buffer.writeIntLE(numOfContentBytesOuterArray);
    }
    // Inner: aa1 + aa2 + aa3
    final int numOfContentBytesInnerArray = 24;
    final double one = 1;
    final double two = 2;
    final double three = 3;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesInnerArray);
    } else {
      buffer.writeIntLE(numOfContentBytesInnerArray);
    }
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeDouble(one);
      buffer.writeDouble(two);
      buffer.writeDouble(three);
    } else {
      buffer.writeDoubleLE(one);
      buffer.writeDoubleLE(two);
      buffer.writeDoubleLE(three);
    }
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesInnerArray);
    } else {
      buffer.writeIntLE(numOfContentBytesInnerArray);
    }
    final byte[] doublePadding = {0x00, 0x00, 0x00, 0x00};
    buffer.writeBytes(doublePadding);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeDouble(one);
      buffer.writeDouble(two);
      buffer.writeDouble(three);
    } else {
      buffer.writeDoubleLE(one);
      buffer.writeDoubleLE(two);
      buffer.writeDoubleLE(three);
    }
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytesInnerArray);
    } else {
      buffer.writeIntLE(numOfContentBytesInnerArray);
    }
    buffer.writeBytes(doublePadding);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeDouble(one);
      buffer.writeDouble(two);
      buffer.writeDouble(three);
    } else {
      buffer.writeDoubleLE(one);
      buffer.writeDoubleLE(two);
      buffer.writeDoubleLE(three);
    }
    final int numOfBytes = buffer.readableBytes();
    final ArrayDecoder<DBusArray<DBusDouble>> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<DBusArray<DBusDouble>>> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<DBusArray<DBusDouble>> array = result.getValue();
    final int expectedArraySize = 3;
    assertEquals(expectedArraySize, array.size(), ASSERT_SIZE_OF_ARRAY);
    final int expectedInnerArraySize = 3;
    assertEquals(expectedInnerArraySize, array.get(1).size(), "Size of inner array");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfSignedShorts(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_SIGNED_SHORTS);
    final ByteBuf buffer = Unpooled.buffer();
    final int numOfContentBytes = 8;
    final short one = 1;
    final short two = 2;
    final short three = 3;
    final short four = 4;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfContentBytes);
      buffer.writeShort(one);
      buffer.writeShort(two);
      buffer.writeShort(three);
      buffer.writeShort(four);
    } else {
      buffer.writeIntLE(numOfContentBytes);
      buffer.writeShortLE(one);
      buffer.writeShortLE(two);
      buffer.writeShortLE(three);
      buffer.writeShortLE(four);
    }
    final int expectedBytes = buffer.readableBytes();
    final ArrayDecoder<Int16> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<Int16>> result = decoder.decode(buffer, 0);
    assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<Int16> array = result.getValue();
    final int expectedArraySize = 4;
    assertEquals(expectedArraySize, array.size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeArrayOfStrings(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_STRINGS);
    final String s1 = "abc1";
    final String s2 = "def2";
    final byte[] s1bytes = s1.getBytes(StandardCharsets.UTF_8);
    final byte[] s2bytes = s2.getBytes(StandardCharsets.UTF_8);
    final ByteBuf stringBuffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      stringBuffer.writeInt(s1bytes.length);
    } else {
      stringBuffer.writeIntLE(s1bytes.length);
    }
    stringBuffer.writeBytes(s1bytes);
    stringBuffer.writeZero(1);
    final byte[] stringPadding = {0x00, 0x00, 0x00};
    stringBuffer.writeBytes(stringPadding);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      stringBuffer.writeInt(s2bytes.length);
    } else {
      stringBuffer.writeIntLE(s2bytes.length);
    }
    stringBuffer.writeBytes(s2bytes);
    stringBuffer.writeZero(1);
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(stringBuffer.readableBytes());
    } else {
      buffer.writeIntLE(stringBuffer.readableBytes());
    }
    buffer.writeBytes(stringBuffer);
    final int numOfBytes = buffer.readableBytes();
    final ArrayDecoder<DBusString> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<DBusString>> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<DBusString> array = result.getValue();
    final int expectedArraySize = 2;
    assertEquals(expectedArraySize, array.size(), ASSERT_SIZE_OF_ARRAY);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeEmptyArrayOfSignedLongs(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_SIGNED_LONGS);
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(0);
    } else {
      buffer.writeIntLE(0);
    }
    final int padding = 4;
    buffer.writeZero(padding);
    final int numOfBytes = buffer.readableBytes();
    final ArrayDecoder<Int64> decoder = new ArrayDecoder<>(byteOrder, signature);
    final DecoderResult<DBusArray<Int64>> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final DBusArray<Int64> array = result.getValue();
    assertTrue(array.isEmpty(), ASSERT_SIZE_OF_ARRAY);
  }
}
