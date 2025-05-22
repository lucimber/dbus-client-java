/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.TypeCode;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class VariantDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
  private static final String BYTE_SIGNATURE = "y";
  private static final String DOUBLE_SIGNATURE = "d";
  private static final String INTEGER_SIGNATURE = "i";
  private static final String TOO_MANY_TYPES = "ii";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void decodeVariantOfByte(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    buffer.writeByte(BYTE_SIGNATURE.length());
    buffer.writeBytes(BYTE_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    buffer.writeZero(1); // NUL byte
    buffer.writeByte(Byte.MAX_VALUE);
    final int expectedBytes = 4;
    final VariantDecoder decoder = new VariantDecoder(byteOrder);
    final DecoderResult<Variant> result = decoder.decode(buffer, 0);
    assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final Variant variant = result.getValue();
    assertEquals(TypeCode.BYTE, variant.getDelegate().getType().getCode());
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void decodeVariantOfDouble(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    buffer.writeByte(DOUBLE_SIGNATURE.length());
    buffer.writeBytes(DOUBLE_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    buffer.writeZero(1); // NUL byte
    final int paddingForDouble = 5;
    buffer.writeZero(paddingForDouble);
    final double testValue = 13.7;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeDouble(testValue);
    } else {
      buffer.writeDoubleLE(testValue);
    }
    final int numOfBytes = buffer.readableBytes();
    final VariantDecoder decoder = new VariantDecoder(byteOrder);
    final DecoderResult<Variant> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final Variant variant = result.getValue();
    assertEquals(TypeCode.DOUBLE, variant.getDelegate().getType().getCode());
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void decodeVariantOfInteger(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    buffer.writeByte(INTEGER_SIGNATURE.length());
    buffer.writeBytes(INTEGER_SIGNATURE.getBytes(StandardCharsets.UTF_8));
    buffer.writeZero(1); // NUL byte
    buffer.writeZero(1); // Pad for INT32
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(Integer.MAX_VALUE);
    } else {
      buffer.writeIntLE(Integer.MAX_VALUE);
    }
    final int expectedBytes = 8;
    final VariantDecoder decoder = new VariantDecoder(ByteOrder.BIG_ENDIAN);
    final DecoderResult<Variant> result = decoder.decode(buffer, 0);
    assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final Variant variant = result.getValue();
    assertEquals(TypeCode.INT32, variant.getDelegate().getType().getCode());
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void failDueToTooManyTypes(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    buffer.writeByte(TOO_MANY_TYPES.length());
    buffer.writeBytes(TOO_MANY_TYPES.getBytes(StandardCharsets.UTF_8));
    buffer.writeZero(1);
    final int firstTestValue = 1024;
    final int secondTestValue = 2048;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(firstTestValue);
      buffer.writeInt(secondTestValue);
    } else {
      buffer.writeIntLE(firstTestValue);
      buffer.writeIntLE(secondTestValue);
    }
    final VariantDecoder decoder = new VariantDecoder(byteOrder);
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
