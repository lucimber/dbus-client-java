/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.DBusByte;
import com.lucimber.dbus.protocol.types.Dict;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.protocol.types.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class DictionaryDecoderTest {

  private static final String ARRAY_OF_ENTRIES = "a{yv}";
  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void decodeDictionary(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_ENTRIES);
    final ByteBuf buffer = Unpooled.buffer();
    final int numOfBytes = 12;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(numOfBytes);
    } else {
      buffer.writeIntLE(numOfBytes);
    }
    final byte[] dictEntryPadding = {0x00, 0x00, 0x00, 0x00};
    buffer.writeBytes(dictEntryPadding);
    // Byte (y)
    buffer.writeByte(Byte.MAX_VALUE);
    // Variant (v)
    final String rawSignature = "i";
    buffer.writeByte(rawSignature.length());
    buffer.writeBytes(rawSignature.getBytes(StandardCharsets.UTF_8));
    buffer.writeZero(1); // NUL byte
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(Integer.MAX_VALUE);
    } else {
      buffer.writeIntLE(Integer.MAX_VALUE);
    }
    final int expectedBytes = 16;
    final DictDecoder<DBusByte, Variant> decoder = new DictDecoder<>(byteOrder, signature);
    final DecoderResult<Dict<DBusByte, Variant>> result = decoder.decode(buffer, 0);
    assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final Dict<DBusByte, Variant> dict = result.getValue();
    assertEquals(1, dict.size());
    assertTrue(dict.containsKey(DBusByte.valueOf(Byte.MAX_VALUE)));
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void decodeEmptyDictionary(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf(ARRAY_OF_ENTRIES);
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(0);
    } else {
      buffer.writeIntLE(0);
    }
    final byte[] dictEntryPadding = {0x00, 0x00, 0x00, 0x00};
    buffer.writeBytes(dictEntryPadding);
    final int numOfBytes = buffer.readableBytes();
    final DictDecoder<DBusByte, Variant> decoder = new DictDecoder<>(byteOrder, signature);
    final DecoderResult<Dict<DBusByte, Variant>> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final Dict<DBusByte, Variant> dict = result.getValue();
    assertTrue(dict.isEmpty());
    buffer.release();
  }
}
