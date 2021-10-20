package io.lucimber.dbus.impl.netty.decoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusDouble;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DoubleDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @Test
    void decodeDoubleOnBigEndian() {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0x40);
        buffer.writeByte(0x02);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        final double expected = 2.3;
        final DoubleDecoder decoder = new DoubleDecoder(ByteOrder.BIG_ENDIAN);
        final DecoderResult<DBusDouble> result = decoder.decode(buffer, 0);
        assertEquals(8, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        assertEquals(expected, result.getValue().getDelegate());
    }

    @Test
    void decodeDoubleOnLittleEndian() {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x66);
        buffer.writeByte(0x02);
        buffer.writeByte(0x40);
        final double expected = 2.3;
        final DoubleDecoder decoder = new DoubleDecoder(ByteOrder.LITTLE_ENDIAN);
        final DecoderResult<DBusDouble> result = decoder.decode(buffer, 0);
        assertEquals(8, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        assertEquals(expected, result.getValue().getDelegate());
    }
}
