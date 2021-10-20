package io.lucimber.dbus.impl.netty.decoder;

import io.lucimber.dbus.type.DBusByte;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ByteDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @Test
    void decodeByte() {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(0xFF);
        final ByteDecoder decoder = new ByteDecoder();
        final DecoderResult<DBusByte> result = decoder.decode(buffer, 0);
        assertEquals(1, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final byte expected = -1;
        assertEquals(expected, result.getValue().getDelegate());
    }
}
