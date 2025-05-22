/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DictEntry;
import com.lucimber.dbus.type.Int64;
import com.lucimber.dbus.type.Signature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class DictEntryEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeSimpleDictEntry(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("{xb}");
    final Encoder<DictEntry<Int64, DBusBoolean>, ByteBuf> encoder =
            new DictEntryEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final DictEntry<Int64, DBusBoolean> dictEntry =
            new DictEntry<>(signature, Int64.valueOf(Long.MIN_VALUE), DBusBoolean.valueOf(false));
    final EncoderResult<ByteBuf> result = encoder.encode(dictEntry, 0);
    final int expectedBytes = 12; // long(8) + boolean(4) ... padding between is zero
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeSimpleDictEntryWithOffset(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("{xb}");
    final Encoder<DictEntry<Int64, DBusBoolean>, ByteBuf> encoder =
            new DictEntryEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final DictEntry<Int64, DBusBoolean> dictEntry =
            new DictEntry<>(signature, Int64.valueOf(Long.MIN_VALUE), DBusBoolean.valueOf(false));
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(dictEntry, offset);
    final int expectedBytes = 15; // 3 bytes + long(8) + boolean(4) ... padding between is zero
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }
}
