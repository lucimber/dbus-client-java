package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.type.Signature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SignatureEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";
    private static final String COMPLEX_VALID_SIGNATURE = "a(bi)aaa{sb}uv(bh(ig))(qat)v";
    private static final String INVALID_BRACKET_SIGNATURE = "a(ia(ii)";
    private static final String INVALID_CHAR_SIGNATURE = "z(i)";
    private static final String VALID_SIGNATURE = "a(ii)";

    @Test
    void encodeValidSignature() {
        final Signature signature = Signature.valueOf(VALID_SIGNATURE);
        final Encoder<Signature, ByteBuf> encoder = new SignatureEncoder(ByteBufAllocator.DEFAULT);
        final EncoderResult<ByteBuf> result = encoder.encode(signature, 0);
        final int expectedBytes = 7;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        final int length = VALID_SIGNATURE.length();
        assertEquals(length, buffer.readByte(), "Signature length");
        buffer.skipBytes(length);
        assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
        buffer.release();
    }

    @Test
    void encodeValidComplexSignature() {
        final Signature signature = Signature.valueOf(COMPLEX_VALID_SIGNATURE);
        final Encoder<Signature, ByteBuf> encoder = new SignatureEncoder(ByteBufAllocator.DEFAULT);
        final EncoderResult<ByteBuf> result = encoder.encode(signature, 0);
        final int expectedBytes = 30;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        final int length = COMPLEX_VALID_SIGNATURE.length();
        assertEquals(length, buffer.readByte(), "Signature length");
        buffer.skipBytes(length);
        assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
        buffer.release();
    }

    @Test
    void failDueToInvalidChar() {
        final Encoder<Signature, ByteBuf> encoder = new SignatureEncoder(ByteBufAllocator.DEFAULT);
        assertThrows(Exception.class, () -> {
            final Signature signature = Signature.valueOf(INVALID_CHAR_SIGNATURE);
            encoder.encode(signature, 0);
        });
    }

    @Test
    void failDueToInvalidBracketCount() {
        final Encoder<Signature, ByteBuf> encoder = new SignatureEncoder(ByteBufAllocator.DEFAULT);
        assertThrows(Exception.class, () -> {
            final Signature signature = Signature.valueOf(INVALID_BRACKET_SIGNATURE);
            encoder.encode(signature, 0);
        });
    }
}
