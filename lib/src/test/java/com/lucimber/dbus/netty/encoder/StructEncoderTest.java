/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.netty.ByteOrder;
import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.Int32;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class StructEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeSimpleStruct(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("(ibv)");
    final Encoder<Struct, ByteBuf> encoder = new StructEncoder(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final Int32 int32 = Int32.valueOf(1);
    final DBusBoolean dBusBoolean = DBusBoolean.valueOf(true);
    final Variant variant = Variant.valueOf(DBusDouble.valueOf(2.5));
    final Struct struct = new Struct(signature, int32, dBusBoolean, variant);
    final EncoderResult<ByteBuf> result = encoder.encode(struct, 0);
    final int expectedBytes = 24; // 4 byte + 4 byte + 3 byte + 5 byte + 8 byte
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeSimpleStructWithOffset(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("(ibv)");
    final Encoder<Struct, ByteBuf> encoder = new StructEncoder(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final Int32 int32 = Int32.valueOf(1);
    final DBusBoolean dBusBoolean = DBusBoolean.valueOf(true);
    final Variant variant = Variant.valueOf(DBusDouble.valueOf(2.5));
    final Struct struct = new Struct(signature, int32, dBusBoolean, variant);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(struct, offset);
    final int expectedBytes = 27; // 3 byte + 4 byte + 4 byte + 3 byte + 5 byte + 8 byte
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }
}
