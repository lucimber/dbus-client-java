package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int16;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ShortDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeSignedShort(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(0x80);
            buffer.writeByte(0x00);
        } else {
            buffer.writeByte(0x00);
            buffer.writeByte(0x80);
        }
        final short expected = -32768;
        final Int16Decoder decoder = new Int16Decoder(byteOrder);
        final DecoderResult<Int16> result = decoder.decode(buffer, 0);
        assertEquals(2, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        assertEquals(expected, result.getValue().getDelegate());
    }
}
