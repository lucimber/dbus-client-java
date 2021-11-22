package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusDouble;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DoubleEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @Test
    void encodeDoubleOnBigEndian() {
        final Encoder<DBusDouble, ByteBuf> encoder =
                new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
        final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, 0);
        final int expectedBytes = 8;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x40, buffer.getByte(0));
        assertEquals((byte) 0x02, buffer.getByte(1));
        assertEquals((byte) 0x66, buffer.getByte(2));
        assertEquals((byte) 0x66, buffer.getByte(3));
        assertEquals((byte) 0x66, buffer.getByte(4));
        assertEquals((byte) 0x66, buffer.getByte(5));
        assertEquals((byte) 0x66, buffer.getByte(6));
        assertEquals((byte) 0x66, buffer.getByte(7));
        buffer.release();
    }

    @Test
    void encodeDoubleWithOffsetOnBigEndian() {
        final Encoder<DBusDouble, ByteBuf> encoder =
                new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.BIG_ENDIAN);
        final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, offset);
        final int expectedBytes = 11;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.getByte(0));
        assertEquals((byte) 0x00, buffer.getByte(1));
        assertEquals((byte) 0x00, buffer.getByte(2));
        assertEquals((byte) 0x40, buffer.getByte(3));
        assertEquals((byte) 0x02, buffer.getByte(4));
        assertEquals((byte) 0x66, buffer.getByte(5));
        assertEquals((byte) 0x66, buffer.getByte(6));
        assertEquals((byte) 0x66, buffer.getByte(7));
        assertEquals((byte) 0x66, buffer.getByte(8));
        assertEquals((byte) 0x66, buffer.getByte(9));
        assertEquals((byte) 0x66, buffer.getByte(10));
        buffer.release();
    }

    @Test
    void encodeDoubleOnLittleEndian() {
        final Encoder<DBusDouble, ByteBuf> encoder =
                new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
        final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, 0);
        final int expectedBytes = 8;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x66, buffer.getByte(0));
        assertEquals((byte) 0x66, buffer.getByte(1));
        assertEquals((byte) 0x66, buffer.getByte(2));
        assertEquals((byte) 0x66, buffer.getByte(3));
        assertEquals((byte) 0x66, buffer.getByte(4));
        assertEquals((byte) 0x66, buffer.getByte(5));
        assertEquals((byte) 0x02, buffer.getByte(6));
        assertEquals((byte) 0x40, buffer.getByte(7));
        buffer.release();
    }

    @Test
    void encodeDoubleWithOffsetOnLittleEndian() {
        final Encoder<DBusDouble, ByteBuf> encoder =
                new DoubleEncoder(ByteBufAllocator.DEFAULT, ByteOrder.LITTLE_ENDIAN);
        final DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusDouble, offset);
        final int expectedBytes = 11;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.getByte(0));
        assertEquals((byte) 0x00, buffer.getByte(1));
        assertEquals((byte) 0x00, buffer.getByte(2));
        assertEquals((byte) 0x66, buffer.getByte(3));
        assertEquals((byte) 0x66, buffer.getByte(4));
        assertEquals((byte) 0x66, buffer.getByte(5));
        assertEquals((byte) 0x66, buffer.getByte(6));
        assertEquals((byte) 0x66, buffer.getByte(7));
        assertEquals((byte) 0x66, buffer.getByte(8));
        assertEquals((byte) 0x02, buffer.getByte(9));
        assertEquals((byte) 0x40, buffer.getByte(10));
        buffer.release();
    }
}
