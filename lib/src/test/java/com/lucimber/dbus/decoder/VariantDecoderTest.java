/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.TypeCode;
import com.lucimber.dbus.type.Variant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class VariantDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
  private static final String BYTE_SIGNATURE = "y";
  private static final String DOUBLE_SIGNATURE = "d";
  private static final String INTEGER_SIGNATURE = "i";
  private static final String TOO_MANY_TYPES = "ii";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeVariantOfByte(ByteOrder byteOrder) throws DecoderException {
    ByteBuffer buffer = ByteBuffer.allocate(4).order(byteOrder);
    buffer.put((byte) BYTE_SIGNATURE.length());
    buffer.put(BYTE_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    buffer.put((byte) 0); // NUL
    buffer.put((byte) 0x7F); // Byte.MAX_VALUE
    buffer.flip();

    Decoder<ByteBuffer, Variant> decoder = new VariantDecoder();
    DecoderResult<Variant> result = decoder.decode(buffer, 0);

    assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertEquals(TypeCode.BYTE, result.getValue().getDelegate().getType().getCode());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeVariantOfDouble(final ByteOrder byteOrder) throws DecoderException {
    // 1 (length byte) + 1 (signature byte) + 1 (NUL) + 5 (padding) + 8 (double)
    ByteBuffer buffer = ByteBuffer.allocate(16).order(byteOrder);
    buffer.put((byte) DOUBLE_SIGNATURE.length());
    buffer.put(DOUBLE_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    buffer.put((byte) 0); // NUL byte
    buffer.put(new byte[5]); // 5 bytes padding for 8-byte alignment
    double value = 13.7;
    long raw = Double.doubleToRawLongBits(value);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.putLong(raw);
    } else {
      buffer.putLong(Long.reverseBytes(raw));
    }
    buffer.flip();

    Decoder<ByteBuffer, Variant> decoder = new VariantDecoder();
    DecoderResult<Variant> result = decoder.decode(buffer, 0);

    assertEquals(16, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertEquals(TypeCode.DOUBLE, result.getValue().getDelegate().getType().getCode());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeVariantOfInteger(ByteOrder byteOrder) throws DecoderException {
    ByteBuffer buffer = ByteBuffer.allocate(8).order(byteOrder);
    buffer.put((byte) INTEGER_SIGNATURE.length());
    buffer.put(INTEGER_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    buffer.put((byte) 0); // NUL
    buffer.put((byte) 0); // padding
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.putInt(Integer.MAX_VALUE);
    } else {
      buffer.putInt(Integer.reverseBytes(Integer.MAX_VALUE));
    }
    buffer.flip();

    Decoder<ByteBuffer, Variant> decoder = new VariantDecoder();
    DecoderResult<Variant> result = decoder.decode(buffer, 0);

    assertEquals(8, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertEquals(TypeCode.INT32, result.getValue().getDelegate().getType().getCode());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void failDueToTooManyTypes(ByteOrder byteOrder) {
    ByteBuffer buffer = ByteBuffer.allocate(12).order(byteOrder);
    buffer.put((byte) TOO_MANY_TYPES.length());
    buffer.put(TOO_MANY_TYPES.getBytes(StandardCharsets.UTF_8));
    buffer.put((byte) 0); // NUL
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.putInt(1024);
      buffer.putInt(2048);
    } else {
      buffer.putInt(Integer.reverseBytes(1024));
      buffer.putInt(Integer.reverseBytes(2048));
    }
    buffer.flip();

    Decoder<ByteBuffer, Variant> decoder = new VariantDecoder();
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
