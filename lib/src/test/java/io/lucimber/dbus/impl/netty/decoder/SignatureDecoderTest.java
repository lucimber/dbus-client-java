package io.lucimber.dbus.impl.netty.decoder;

import io.lucimber.dbus.type.Signature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SignatureDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    private static final String COMPLEX_VALID_SIGNATURE = "a(bi)aaa{sb}uv(bh(ig))(qat)v";
    private static final String INVALID_BRACKET_SIGNATURE = "a(ia(ii)}";
    private static final String INVALID_CHAR_SIGNATURE = "z(i)";
    private static final String VALID_SIGNATURE = "a(ii)b";

    @Test
    void decodeValidSignature() {
        final ByteBuf buffer = Unpooled.buffer();
        // UBYTE
        buffer.writeByte(VALID_SIGNATURE.length());
        // UTF-8 bytes (6 bytes)
        buffer.writeBytes(VALID_SIGNATURE.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final int numOfBytes = buffer.readableBytes();
        final SignatureDecoder decoder = new SignatureDecoder();
        final DecoderResult<Signature> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Signature signature = result.getValue();
        assertEquals(VALID_SIGNATURE, signature.toString());
        assertEquals(2, signature.getQuantity());
    }

    @Test
    void decodeComplexValidSignature() {
        final ByteBuf buffer = Unpooled.buffer();
        // UBYTE
        buffer.writeByte(COMPLEX_VALID_SIGNATURE.length());
        // UTF-8 bytes (28 bytes)
        buffer.writeBytes(COMPLEX_VALID_SIGNATURE.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final int numOfBytes = buffer.readableBytes();
        final SignatureDecoder decoder = new SignatureDecoder();
        final DecoderResult<Signature> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Signature signature = result.getValue();
        assertEquals(COMPLEX_VALID_SIGNATURE, signature.toString());
        assertEquals(7, signature.getQuantity());
    }

    @Test
    void decodeInvalidCharSignature() {
        final ByteBuf buffer = Unpooled.buffer();
        // UBYTE
        buffer.writeByte(INVALID_CHAR_SIGNATURE.length());
        // UTF-8 bytes (4 bytes)
        buffer.writeBytes(INVALID_CHAR_SIGNATURE.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final SignatureDecoder decoder = new SignatureDecoder();
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }

    @Test
    void decodeInvalidBracketSignature() {
        final ByteBuf buffer = Unpooled.buffer();
        // UBYTE
        buffer.writeByte(INVALID_BRACKET_SIGNATURE.length());
        // UTF-8 bytes (9 bytes)
        buffer.writeBytes(INVALID_BRACKET_SIGNATURE.getBytes(StandardCharsets.UTF_8));
        // Trailing NUL byte
        buffer.writeByte(0x00);
        final SignatureDecoder decoder = new SignatureDecoder();
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }
}
