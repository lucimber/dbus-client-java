/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VariantEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeVariantOfSignedInteger(ByteOrder byteOrder) {
  DBusVariant variant = DBusVariant.valueOf(DBusInt32.valueOf(Integer.MAX_VALUE));
  Encoder<DBusVariant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
  EncoderResult<ByteBuffer> result = encoder.encode(variant, 0);
  int expectedBytes = 8; // 3 byte + 1 byte + 4 byte
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeVariantOfSignedIntegerWithOffset(ByteOrder byteOrder) {
  DBusVariant variant = DBusVariant.valueOf(DBusInt32.valueOf(Integer.MAX_VALUE));
  Encoder<DBusVariant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
  int offset = 5;
  EncoderResult<ByteBuffer> result = encoder.encode(variant, offset);
  int expectedBytes = 7; // 3 byte + 4 byte
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeVariantOfArray(ByteOrder byteOrder) {
  DBusSignature signature = DBusSignature.valueOf("at");
  DBusArray<DBusUInt64> array = new DBusArray<>(signature);
  array.add(DBusUInt64.valueOf(Long.MAX_VALUE));
  DBusVariant variant = DBusVariant.valueOf(array);
  Encoder<DBusVariant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
  EncoderResult<ByteBuffer> result = encoder.encode(variant, 0);
  int expectedBytes = 16; // 4 byte + 4 byte + 8 byte
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodingVariantOfStruct(ByteOrder byteOrder) {
  DBusSignature structSig = DBusSignature.valueOf("(aqo)");
  DBusSignature arraySig = DBusSignature.valueOf("aq");
  DBusArray<DBusUInt16> dBusArray = new DBusArray<>(arraySig);
  dBusArray.add(DBusUInt16.valueOf(Short.MIN_VALUE));
  dBusArray.add(DBusUInt16.valueOf(Short.MAX_VALUE));
  DBusObjectPath objectPath = DBusObjectPath.valueOf("/junit_object_path");
  DBusStruct struct = new DBusStruct(structSig, dBusArray, objectPath);
  DBusVariant variant = DBusVariant.valueOf(struct);
  Encoder<DBusVariant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
  EncoderResult<ByteBuffer> result = encoder.encode(variant, 0);
  int expectedBytes = 39; // 3 byte + 5 byte + 8 byte + 0 byte + 23 byte
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }
}
