/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.type.DBusByte;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

final class ByteDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @Test
  void decodeByte() {
    final ByteBuf buffer = Unpooled.buffer();
    final byte maxValue = (byte) 0xFF;
    buffer.writeByte(maxValue);
    final ByteDecoder decoder = new ByteDecoder();
    final DecoderResult<DBusByte> result = decoder.decode(buffer, 0);
    assertEquals(1, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    final byte expected = -1;
    assertEquals(expected, result.getValue().getDelegate());
  }
}
