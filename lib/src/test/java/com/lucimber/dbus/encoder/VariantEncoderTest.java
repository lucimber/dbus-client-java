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

final class VariantEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeVariantOfSignedInteger(ByteOrder byteOrder) {
    Variant variant = Variant.valueOf(Int32.valueOf(Integer.MAX_VALUE));
    Encoder<Variant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
    EncoderResult<ByteBuffer> result = encoder.encode(variant, 0);
    int expectedBytes = 8; // 3 byte + 1 byte + 4 byte
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeVariantOfSignedIntegerWithOffset(ByteOrder byteOrder) {
    Variant variant = Variant.valueOf(Int32.valueOf(Integer.MAX_VALUE));
    Encoder<Variant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
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
    Signature signature = Signature.valueOf("at");
    DBusArray<UInt64> array = new DBusArray<>(signature);
    array.add(UInt64.valueOf(Long.MAX_VALUE));
    Variant variant = Variant.valueOf(array);
    Encoder<Variant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
    EncoderResult<ByteBuffer> result = encoder.encode(variant, 0);
    int expectedBytes = 16; // 4 byte + 4 byte + 8 byte
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodingVariantOfStruct(ByteOrder byteOrder) {
    Signature structSig = Signature.valueOf("(aqo)");
    Signature arraySig = Signature.valueOf("aq");
    DBusArray<UInt16> dBusArray = new DBusArray<>(arraySig);
    dBusArray.add(UInt16.valueOf(Short.MIN_VALUE));
    dBusArray.add(UInt16.valueOf(Short.MAX_VALUE));
    ObjectPath objectPath = ObjectPath.valueOf("/junit_object_path");
    Struct struct = new Struct(structSig, dBusArray, objectPath);
    Variant variant = Variant.valueOf(struct);
    Encoder<Variant, ByteBuffer> encoder = new VariantEncoder(byteOrder);
    EncoderResult<ByteBuffer> result = encoder.encode(variant, 0);
    int expectedBytes = 39; // 3 byte + 5 byte + 8 byte + 0 byte + 23 byte
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }
}
