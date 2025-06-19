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

final class StructEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSimpleStruct(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("(ibv)");
    Encoder<Struct, ByteBuffer> encoder = new StructEncoder(byteOrder, signature);
    Int32 int32 = Int32.valueOf(1);
    DBusBoolean dBusBoolean = DBusBoolean.valueOf(true);
    Variant variant = Variant.valueOf(DBusDouble.valueOf(2.5));
    Struct struct = new Struct(signature, int32, dBusBoolean, variant);
    EncoderResult<ByteBuffer> result = encoder.encode(struct, 0);
    int expectedBytes = 24; // 4 byte + 4 byte + 3 byte + 5 byte + 8 byte
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeSimpleStructWithOffset(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("(ibv)");
    Encoder<Struct, ByteBuffer> encoder = new StructEncoder(byteOrder, signature);
    Int32 int32 = Int32.valueOf(1);
    DBusBoolean dBusBoolean = DBusBoolean.valueOf(true);
    Variant variant = Variant.valueOf(DBusDouble.valueOf(2.5));
    Struct struct = new Struct(signature, int32, dBusBoolean, variant);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(struct, offset);

    // Offset 5 → +3 struct padding → start at 8
    // Int32: 4 bytes (8–11)
    // Boolean: 4 bytes (12–15)
    // Variant:
    //   - Signature: 3 bytes ("d" + NUL)
    //   - Padding: 5 bytes (align to 8-byte boundary at 24)
    //   - Double: 8 bytes (24–31)
    int expectedBytes = 3 + 4 + 4 + 3 + 5 + 8; // = 27

    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }
}
