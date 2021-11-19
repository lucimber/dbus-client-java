package io.lucimber.dbus.impl.netty.decoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.ObjectPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ObjectPathDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
    private static final String INVALID_OBJECT_PATH = "/a//c";
    private static final String VALID_OBJECT_PATH = "/abc/d1/e_f";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeObjectPath(final ByteOrder byteOrder) throws DecoderException {
        final ByteBuf buffer = Unpooled.buffer();
        // UINT32 bytes
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x0B);
        } else {
            buffer.writeByte(0x0B);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
        }
        // UTF-8 bytes (11 bytes)
        buffer.writeBytes(VALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final int numOfBytes = buffer.readableBytes();
        final ObjectPathDecoder decoder = new ObjectPathDecoder(byteOrder);
        final DecoderResult<ObjectPath> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        assertEquals(VALID_OBJECT_PATH, result.getValue().getWrappedValue());
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void failDueToIndexLimitation(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        // UINT32 bytes (Integer.MAX_VALUE + 1 = 2147483648)
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(0x80);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
        } else {
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x80);
        }
        // UTF-8 bytes (12 bytes)
        buffer.writeBytes(VALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final ObjectPathDecoder decoder = new ObjectPathDecoder(byteOrder);
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void failDueToInvalidObjectPath(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        // UINT32 bytes
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x05);
        } else {
            buffer.writeByte(0x05);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
            buffer.writeByte(0x00);
        }
        // UTF-8 bytes (5 bytes)
        buffer.writeBytes(INVALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final ObjectPathDecoder decoder = new ObjectPathDecoder(byteOrder);
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }
}
