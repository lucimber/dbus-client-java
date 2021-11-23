package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int16;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShortEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMaxValue(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 int16 = Int16.valueOf(Short.MAX_VALUE);
        final EncoderResult<ByteBuf> result = encoder.encode(int16, 0);
        final int expectedNumOfBytes = 2;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            final byte[] expectedBytes = {0x7F, (byte) 0xFF};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
        } else {
            final byte[] expectedBytes = {(byte) 0xFF, 0x7F};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
        }
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMaxValueWithOffset(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 int16 = Int16.valueOf(Short.MAX_VALUE);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(int16, offset);
        final int expectedNumOfBytes = 3;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            final byte[] expectedBytes = {0x00, 0x7F, (byte) 0xFF};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
        } else {
            final byte[] expectedBytes = {0x00, (byte) 0xFF, 0x7F};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
        }
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMinValue(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 int16 = Int16.valueOf(Short.MIN_VALUE);
        final EncoderResult<ByteBuf> result = encoder.encode(int16, 0);
        final int expectedNumOfBytes = 2;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            final byte[] expectedBytes = {(byte) 0x80, 0x00};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
        } else {
            final byte[] expectedBytes = {0x00, (byte) 0x80};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
        }
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMinValueWithOffset(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 int16 = Int16.valueOf(Short.MIN_VALUE);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(int16, offset);
        final int expectedNumOfBytes = 3;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.readableBytes(), READABLE_BYTES);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            final byte[] expectedBytes = {0x00, (byte) 0x80, 0x00};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
        } else {
            final byte[] expectedBytes = {0x00, 0x00, (byte) 0x80};
            final byte[] actualBytes = new byte[expectedNumOfBytes];
            buffer.getBytes(0, actualBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
        }
        buffer.release();
    }
}
