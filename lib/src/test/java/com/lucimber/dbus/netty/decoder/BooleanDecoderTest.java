/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.netty.ByteOrder;
import com.lucimber.dbus.type.DBusBoolean;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

final class BooleanDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @Test
  void decodeFalseOnBigEndian() throws DecoderException {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x00, 0x00, 0x00, 0x00};
    buffer.writeBytes(bytes);
    final BooleanDecoder decoder = new BooleanDecoder(ByteOrder.BIG_ENDIAN);
    final DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertFalse(result.getValue().getDelegate());
  }

  @Test
  void decodeFalseOnLittleEndian() throws DecoderException {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x00, 0x00, 0x00, 0x00};
    buffer.writeBytes(bytes);
    final BooleanDecoder decoder = new BooleanDecoder(ByteOrder.LITTLE_ENDIAN);
    final DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertFalse(result.getValue().getDelegate());
  }

  @Test
  void decodeTrueOnBigEndian() throws DecoderException {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x00, 0x00, 0x00, 0x01};
    buffer.writeBytes(bytes);
    final BooleanDecoder decoder = new BooleanDecoder(ByteOrder.BIG_ENDIAN);
    final DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertTrue(result.getValue().getDelegate());
  }

  @Test
  void decodeTrueOnLittleEndian() throws DecoderException {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x01, 0x00, 0x00, 0x00};
    buffer.writeBytes(bytes);
    final BooleanDecoder decoder = new BooleanDecoder(ByteOrder.LITTLE_ENDIAN);
    final DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertTrue(result.getValue().getDelegate());
  }

  @Test
  void failOnBigEndian() {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x01, 0x00, 0x00, 0x01};
    buffer.writeBytes(bytes);
    final BooleanDecoder decoder = new BooleanDecoder(ByteOrder.BIG_ENDIAN);
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }

  @Test
  void failOnLittleEndian() {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x01, 0x00, 0x00, 0x01};
    buffer.writeBytes(bytes);
    final BooleanDecoder decoder = new BooleanDecoder(ByteOrder.BIG_ENDIAN);
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
