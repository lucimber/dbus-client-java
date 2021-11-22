package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.UnixFd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class UnixFileDescriptorDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeUnixFileDescriptor(final ByteOrder byteOrder) throws DecoderException {
        final ByteBuf buffer = Unpooled.buffer();
        final int value = 0xFFFFFC00;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(value);
        } else {
            buffer.writeIntLE(value);
        }
        final int expected = -1024;
        final Decoder<ByteBuf, UnixFd> decoder = new UnixFdDecoder(byteOrder);
        final DecoderResult<UnixFd> result = decoder.decode(buffer, 0);
        assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final UnixFd descriptor = result.getValue();
        assertEquals(expected, descriptor.getDelegate());
    }
}
