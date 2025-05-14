/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.protocol.types.DBusByte;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

final class ByteEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @Test
  void encodeByteOfMinValue() {
    final Encoder<DBusByte, ByteBuf> encoder = new ByteEncoder(ByteBufAllocator.DEFAULT);
    final DBusByte dbusByte = DBusByte.valueOf((byte) 0);
    final EncoderResult<ByteBuf> result = encoder.encode(dbusByte, 0);
    final int expectedBytes = 1;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    assertEquals((byte) 0x00, buffer.getByte(0));
    buffer.release();
  }

  @Test
  void encodeByteOfMaxValue() {
    final Encoder<DBusByte, ByteBuf> encoder = new ByteEncoder(ByteBufAllocator.DEFAULT);
    final DBusByte dbusByte = DBusByte.valueOf((byte) 255);
    final EncoderResult<ByteBuf> result = encoder.encode(dbusByte, 0);
    final int expectedBytes = 1;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte byteMaxValue = (byte) 0xFF;
    assertEquals(byteMaxValue, buffer.getByte(0));
    buffer.release();
  }
}
