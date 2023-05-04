/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusDouble;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

final class DoubleEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @Test
  void encodeDoubleOnBigEndian() {
    final Encoder<DBusDouble, ByteBuf> encoder =
            new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
    final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
    final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, 0);
    final int expectedNumOfBytes = 8;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x40, 0x02, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    buffer.release();
  }

  @Test
  void encodeDoubleWithOffsetOnBigEndian() {
    final Encoder<DBusDouble, ByteBuf> encoder =
            new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
    final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, offset);
    final int expectedNumOfBytes = 11;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x40, 0x02, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    buffer.release();
  }

  @Test
  void encodeDoubleOnLittleEndian() {
    final Encoder<DBusDouble, ByteBuf> encoder =
            new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
    final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
    final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, 0);
    final int expectedNumOfBytes = 8;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x02, 0x40};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    buffer.release();
  }

  @Test
  void encodeDoubleWithOffsetOnLittleEndian() {
    final Encoder<DBusDouble, ByteBuf> encoder =
            new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
    final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, offset);
    final int expectedNumOfBytes = 11;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x02, 0x40};
    final byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    buffer.release();
  }
}
