package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StringDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
    private static final String VALID_STRING = "test!ü";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeString(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        // UINT32 bytes
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            final byte[] lengthBytes = {0x00, 0x00, 0x00, 0x07};
            buffer.writeBytes(lengthBytes);
        } else {
            final byte[] lengthBytes = {0x07, 0x00, 0x00, 0x00};
            buffer.writeBytes(lengthBytes);
        }

        // UTF-8 bytes (7 bytes)
        buffer.writeBytes(VALID_STRING.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final int numOfBytes = buffer.readableBytes();
        final StringDecoder decoder = new StringDecoder(byteOrder);
        final DecoderResult<DBusString> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        assertEquals(VALID_STRING, result.getValue().getDelegate());
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void failDueToIndexLimitation(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        // UINT32 bytes (Integer.MAX_VALUE + 1 = 2147483648)
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            final byte[] lengthBytes = {(byte) 0x80, 0x00, 0x00, 0x00};
            buffer.writeBytes(lengthBytes);
        } else {
            final byte[] lengthBytes = {0x00, 0x00, 0x00, (byte) 0x80};
            buffer.writeBytes(lengthBytes);
        }
        // UTF-8 bytes (7 bytes)
        buffer.writeBytes(VALID_STRING.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final StringDecoder decoder = new StringDecoder(byteOrder);
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }
}
