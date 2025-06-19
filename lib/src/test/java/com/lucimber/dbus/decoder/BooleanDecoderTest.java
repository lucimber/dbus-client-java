/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusBoolean;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

final class BooleanDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @Test
  void decodeFalseOnBigEndian() throws DecoderException {
    byte[] bytes = {0x00, 0x00, 0x00, 0x00};
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    BooleanDecoder decoder = new BooleanDecoder();
    DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertFalse(result.getValue().getDelegate());
  }

  @Test
  void decodeFalseOnLittleEndian() throws DecoderException {
    byte[] bytes = {0x00, 0x00, 0x00, 0x00};
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    BooleanDecoder decoder = new BooleanDecoder();
    DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertFalse(result.getValue().getDelegate());
  }

  @Test
  void decodeTrueOnBigEndian() throws DecoderException {
    byte[] bytes = {0x00, 0x00, 0x00, 0x01};
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    BooleanDecoder decoder = new BooleanDecoder();
    DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertTrue(result.getValue().getDelegate());
  }

  @Test
  void decodeTrueOnLittleEndian() throws DecoderException {
    byte[] bytes = {0x01, 0x00, 0x00, 0x00};
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    BooleanDecoder decoder = new BooleanDecoder();
    DecoderResult<DBusBoolean> result = decoder.decode(buffer, 0);
    assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertTrue(result.getValue().getDelegate());
  }

  @Test
  void failOnBigEndian() {
    byte[] bytes = {0x01, 0x00, 0x00, 0x01};
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    BooleanDecoder decoder = new BooleanDecoder();
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }

  @Test
  void failOnLittleEndian() {
    byte[] bytes = {0x01, 0x00, 0x00, 0x01};
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    BooleanDecoder decoder = new BooleanDecoder();
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
