package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int32;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IntegerEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @Test
    void encodeSignedIntegerMinValueOnBigEndian() {
        final Encoder<Int32, ByteBuf> encoder =
                new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
        final Int32 dbusInt32 = Int32.valueOf(-2147483648);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusInt32, 0);
        final int expectedBytes = 4;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x80, buffer.readByte(), "Big Endian");
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        buffer.release();
    }

    @Test
    void encodeSignedIntegerMinValueWithOffsetOnBigEndian() {
        final Encoder<Int32, ByteBuf> encoder =
                new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
        final Int32 dbusInt32 = Int32.valueOf(-2147483648);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusInt32, offset);
        final int expectedBytes = 7;
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
        buffer.release();
    }

    @Test
    void encodeSignedIntegerMinValueOnLittleEndian() {
        final Encoder<Int32, ByteBuf> encoder =
                new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
        final Int32 dbusInt32 = Int32.valueOf(-2147483648);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusInt32, 0);
        final int expectedBytes = 4;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x00, buffer.readByte());
        assertEquals((byte) 0x80, buffer.readByte(), "Little Endian");
        buffer.release();
    }

    @Test
    void encodeSignedIntegerMinValueWithOffsetOnLittleEndian() {
        final Encoder<Int32, ByteBuf> encoder =
                new Int32Encoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
        final Int32 dbusInt32 = Int32.valueOf(-2147483648);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusInt32, offset);
        final int expectedBytes = 7;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
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
