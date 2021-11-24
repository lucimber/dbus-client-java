package com.lucimber.dbus.impl.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusBoolean;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class BooleanEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
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
  @EnumSource(ByteOrder.class)
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
  @EnumSource(ByteOrder.class)
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
  @EnumSource(ByteOrder.class)
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
