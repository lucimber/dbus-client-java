package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LongEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @Test
    void encodeSignedLongMinValueOnBigEndian() {
        final Encoder<Int64, ByteBuf> encoder =
                new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
        final Int64 dbusINT64 = Int64.valueOf(-9223372036854775808L);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT64, 0);
        final int expectedBytes = 8;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x80, buffer.readByte(), "Big Endian");
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        buffer.release();
    }

    @Test
    void encodeSignedLongMinValueWithOffsetOnBigEndian() {
        final Encoder<Int64, ByteBuf> encoder =
                new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
        final Int64 dbusINT64 = Int64.valueOf(-9223372036854775808L);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT64, offset);
        final int expectedBytes = 11;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x80, buffer.readByte(), "Big Endian");
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        buffer.release();
    }

    @Test
    void encodeSignedLongMinValueOnLittleEndian() {
        final Encoder<Int64, ByteBuf> encoder =
                new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
        final Int64 dbusINT64 = Int64.valueOf(-9223372036854775808L);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT64, 0);
        final int expectedBytes = 8;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x80, buffer.readByte(), "Little Endian");
        buffer.release();
    }

    @Test
    void encodeSignedLongMinValueWithOffsetOnLittleEndian() {
        final Encoder<Int64, ByteBuf> encoder =
                new Int64Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
        final Int64 dbusINT64 = Int64.valueOf(-9223372036854775808L);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusINT64, offset);
        final int expectedBytes = 11;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x80, buffer.readByte(), "Little Endian");
        buffer.release();
    }
}
