package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusBoolean;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BooleanEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeBooleanFalse(final ByteOrder byteOrder) {
        final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(false), 0);
        final int expectedBytes = 4;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.getByte(0));
        assertEquals((byte) 0x00, buffer.getByte(1));
        assertEquals((byte) 0x00, buffer.getByte(2));
        assertEquals((byte) 0x00, buffer.getByte(3));
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeBooleanFalseWithOffset(final ByteOrder byteOrder) {
        final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(false), offset);
        final int expectedBytes = 7;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.getByte(0));
        assertEquals((byte) 0x00, buffer.getByte(1));
        assertEquals((byte) 0x00, buffer.getByte(2));
        assertEquals((byte) 0x00, buffer.getByte(3));
        assertEquals((byte) 0x00, buffer.getByte(4));
        assertEquals((byte) 0x00, buffer.getByte(5));
        assertEquals((byte) 0x00, buffer.getByte(6));
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeBooleanTrue(final ByteOrder byteOrder) {
        final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(true), 0);
        final int expectedBytes = 4;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x00, buffer.getByte(0));
        } else {
            assertEquals((byte) 0x01, buffer.getByte(0));
        }
        assertEquals((byte) 0x00, buffer.getByte(1));
        assertEquals((byte) 0x00, buffer.getByte(2));
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x01, buffer.getByte(3));
        } else {
            assertEquals((byte) 0x00, buffer.getByte(3));
        }
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeBooleanTrueWithOffset(final ByteOrder byteOrder) {
        final Encoder<DBusBoolean, ByteBuf> encoder = new BooleanEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(DBusBoolean.valueOf(true), offset);
        final int expectedBytes = 7;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.getByte(0));
        assertEquals((byte) 0x00, buffer.getByte(1));
        assertEquals((byte) 0x00, buffer.getByte(2));
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x00, buffer.getByte(3));
        } else {
            assertEquals((byte) 0x01, buffer.getByte(3));
        }
        assertEquals((byte) 0x00, buffer.getByte(4));
        assertEquals((byte) 0x00, buffer.getByte(5));
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x01, buffer.getByte(6));
        } else {
            assertEquals((byte) 0x00, buffer.getByte(6));
        }
        buffer.release();
    }
}
