/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.Int64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

final class LongEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @Test
  void encodeSignedLongMinValueOnBigEndian() {
    final Encoder<Int64, ByteBuf> encoder = new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
    final Int64 int64 = Int64.valueOf(-9223372036854775808L);
    final EncoderResult<ByteBuf> result = encoder.encode(int64, 0);
    final int expectedNumOfBytes = 8;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    buffer.release();
  }

  @Test
  void encodeSignedLongMinValueWithOffsetOnBigEndian() {
    final Encoder<Int64, ByteBuf> encoder = new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
    final Int64 int64 = Int64.valueOf(-9223372036854775808L);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(int64, offset);
    final int expectedNumOfBytes = 11;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    buffer.release();
  }

  @Test
  void encodeSignedLongMinValueOnLittleEndian() {
    final Encoder<Int64, ByteBuf> encoder = new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
    final Int64 int64 = Int64.valueOf(-9223372036854775808L);
    final EncoderResult<ByteBuf> result = encoder.encode(int64, 0);
    final int expectedNumOfBytes = 8;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    buffer.release();
  }

  @Test
  void encodeSignedLongMinValueWithOffsetOnLittleEndian() {
    final Encoder<Int64, ByteBuf> encoder = new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
    final Int64 int64 = Int64.valueOf(-9223372036854775808L);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(int64, offset);
    final int expectedNumOfBytes = 11;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    buffer.release();
  }
}
