package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.type.DBusByte;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ByteEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @Test
    void encodeByteOfMinValue() {
        final Encoder<DBusByte, ByteBuf> encoder = new ByteEncoder(ByteBufAllocator.DEFAULT);
        final DBusByte dbusByte = DBusByte.valueOf((byte) 0);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusByte, 0);
        final int expectedBytes = 1;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0x00, buffer.getByte(0));
        buffer.release();
    }

    @Test
    void encodeByteOfMaxValue() {
        final Encoder<DBusByte, ByteBuf> encoder = new ByteEncoder(ByteBufAllocator.DEFAULT);
        final DBusByte dbusByte = DBusByte.valueOf((byte) 255);
        final EncoderResult<ByteBuf> result = encoder.encode(dbusByte, 0);
        final int expectedBytes = 1;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        assertEquals((byte) 0xFF, buffer.getByte(0));
        buffer.release();
    }
}
