/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.encoder;

import com.lucimber.dbus.type.DBusBoolean;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class BooleanEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanFalse(final ByteOrder byteOrder) {
    final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(false), 0);
    final int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00};
    final byte[] actualBytes = new byte[buffer.readableBytes()];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    buffer.release();
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanFalseWithOffset(final ByteOrder byteOrder) {
    final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(false), offset);
    final int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final byte[] actualBytes = new byte[buffer.readableBytes()];
    buffer.getBytes(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes);
    buffer.release();
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanTrue(final ByteOrder byteOrder) {
    final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(true), 0);
    final int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x01};
      final byte[] actualBytes = new byte[buffer.readableBytes()];
      buffer.getBytes(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      final byte[] expectedBytes = {0x01, 0x00, 0x00, 0x00};
      final byte[] actualBytes = new byte[buffer.readableBytes()];
      buffer.getBytes(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    buffer.release();
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeBooleanTrueWithOffset(final ByteOrder byteOrder) {
    final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(true), offset);
    final int expectedNumOfBytes = 7;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
      final byte[] actualBytes = new byte[buffer.readableBytes()];
      buffer.getBytes(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
    } else {
      final byte[] expectedBytes = {0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00};
      final byte[] actualBytes = new byte[buffer.readableBytes()];
      buffer.getBytes(0, actualBytes);
      assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
    }
    buffer.release();
  }
}
