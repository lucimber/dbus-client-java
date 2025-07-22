/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusStruct;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StructDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
  private static final String FIRST_COMPLEX_SIGNATURE = "(ayv)";
  private static final String ONE_BYTE_SIGNATURE = "(y)";
  private static final String ONE_DOUBLE_SIGNATURE = "(d)";
  private static final String ONE_INTEGER_SIGNATURE = "(i)";
  private static final String TWO_BYTES_SIGNATURE = "(yy)";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeFirstComplexSignature(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(FIRST_COMPLEX_SIGNATURE);
  ByteBuffer buffer = ByteBuffer.allocate(32).order(byteOrder);
  // Array of bytes
  buffer.putInt(5);
  buffer.put(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05});
  // Variant
  String rawVariantSignature = "i";
  byte[] sigBytes = rawVariantSignature.getBytes(StandardCharsets.UTF_8);
  buffer.put((byte) sigBytes.length);
  buffer.put(sigBytes);
  buffer.put((byte) 0x00);
  buffer.putInt(Integer.MAX_VALUE);
  buffer.flip();

  StructDecoder decoder = new StructDecoder(signature);
  DecoderResult<DBusStruct> result = decoder.decode(buffer, 0);

  assertEquals(buffer.limit(), result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  DBusStruct struct = result.getValue();
  assertEquals(FIRST_COMPLEX_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
  assertEquals(2, struct.getDelegate().size(), "Elements in struct");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeStructOfOneByte(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ONE_BYTE_SIGNATURE);
  ByteBuffer buffer = ByteBuffer.allocate(1).order(byteOrder);
  buffer.put((byte) 0x7F);
  buffer.flip();

  StructDecoder decoder = new StructDecoder(signature);
  DecoderResult<DBusStruct> result = decoder.decode(buffer, 0);

  assertEquals(1, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  DBusStruct struct = result.getValue();
  assertEquals(ONE_BYTE_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
  assertEquals(1, struct.getDelegate().size(), "Elements in struct");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeStructOfTwoBytes(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(TWO_BYTES_SIGNATURE);
  ByteBuffer buffer = ByteBuffer.allocate(2).order(byteOrder);
  buffer.put(Byte.MAX_VALUE);
  buffer.put(Byte.MIN_VALUE);
  buffer.flip();

  StructDecoder decoder = new StructDecoder(signature);
  DecoderResult<DBusStruct> result = decoder.decode(buffer, 0);

  assertEquals(2, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  DBusStruct struct = result.getValue();
  assertEquals(TWO_BYTES_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
  assertEquals(2, struct.getDelegate().size(), "Elements in struct");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeStructOfOneDouble(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ONE_DOUBLE_SIGNATURE);
  ByteBuffer buffer = ByteBuffer.allocate(8).order(byteOrder);
  long rawBits = Double.doubleToRawLongBits(Double.MAX_VALUE);
  if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.putLong(rawBits);
  } else {
      buffer.putLong(Long.reverseBytes(rawBits));
  }
  buffer.flip();

  StructDecoder decoder = new StructDecoder(signature);
  DecoderResult<DBusStruct> result = decoder.decode(buffer, 0);

  assertEquals(8, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  DBusStruct struct = result.getValue();
  assertEquals(ONE_DOUBLE_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
  assertEquals(1, struct.getDelegate().size(), "Elements in struct");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodingStructOfOneInteger(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf(ONE_INTEGER_SIGNATURE);
  ByteBuffer buffer = ByteBuffer.allocate(4).order(byteOrder);
  buffer.putInt(Integer.MAX_VALUE);
  buffer.flip();

  StructDecoder decoder = new StructDecoder(signature);
  DecoderResult<DBusStruct> result = decoder.decode(buffer, 0);

  assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  DBusStruct struct = result.getValue();
  assertEquals(ONE_INTEGER_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
  assertEquals(1, struct.getDelegate().size(), "Elements in struct");
  }
}
