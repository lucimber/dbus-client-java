package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.Int16;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShortEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMaxValue(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 dbusINT16 = Int16.valueOf(Short.MAX_VALUE);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT16, 0);
        final int expectedBytes = 2;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x7F, buffer.readByte());
            assertEquals((byte) 0xFF, buffer.readByte());
        } else {
            assertEquals((byte) 0xFF, buffer.readByte());
            assertEquals((byte) 0x7F, buffer.readByte());
        }
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMaxValueWithOffset(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 dbusINT16 = Int16.valueOf(Short.MAX_VALUE);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT16, offset);
        final int expectedBytes = 3;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.readByte());
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x7F, buffer.readByte());
            assertEquals((byte) 0xFF, buffer.readByte());
        } else {
            assertEquals((byte) 0xFF, buffer.readByte());
            assertEquals((byte) 0x7F, buffer.readByte());
        }
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMinValue(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 dbusINT16 = Int16.valueOf(Short.MIN_VALUE);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT16, 0);
        final int expectedBytes = 2;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x80, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
        } else {
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x80, buffer.readByte());
        }
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSignedShortMinValueWithOffset(final ByteOrder byteOrder) {
        final Encoder<Int16, ByteBuf> encoder = new Int16Encoder(ByteBufAllocator.DEFAULT, byteOrder);
        final Int16 dbusINT16 = Int16.valueOf(Short.MIN_VALUE);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT16, offset);
        final int expectedBytes = 3;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.readByte());
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x80, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
        } else {
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x80, buffer.readByte());
        }
        buffer.release();
    }
}
