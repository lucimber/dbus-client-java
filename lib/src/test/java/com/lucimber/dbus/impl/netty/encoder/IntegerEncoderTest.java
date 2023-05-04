/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int32;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

final class IntegerEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @Test
  void encodeSignedIntegerMinValueOnBigEndian() {
    final Encoder<Int32, ByteBuf> encoder = new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
    final Int32 int32 = Int32.valueOf(-2147483648);
    final EncoderResult<ByteBuf> result = encoder.encode(int32, 0);
    final int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {(byte) 0x80, 0x00, 0x00, 0x00};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    buffer.release();
  }

  @Test
  void encodeSignedIntegerMinValueWithOffsetOnBigEndian() {
    final Encoder<Int32, ByteBuf> encoder = new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
    final Int32 int32 = Int32.valueOf(-2147483648);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(int32, offset);
    final int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, (byte) 0x80, 0x00, 0x00, 0x00};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    buffer.release();
  }

  @Test
  void encodeSignedIntegerMinValueOnLittleEndian() {
    final Encoder<Int32, ByteBuf> encoder = new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
    final Int32 int32 = Int32.valueOf(-2147483648);
    final EncoderResult<ByteBuf> result = encoder.encode(int32, 0);
    final int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, (byte) 0x80};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    buffer.release();
  }

  @Test
  void encodeSignedIntegerMinValueWithOffsetOnLittleEndian() {
    final Encoder<Int32, ByteBuf> encoder = new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
    final Int32 int32 = Int32.valueOf(-2147483648);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(int32, offset);
    final int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    buffer.release();
  }
}
