package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DictEntry;
import com.lucimber.dbus.type.Int32;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.SignatureException;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class EntryDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
    private static final String INVALID_KEY_ARRAY = "ad";
    private static final String INVALID_KEY_DICT_ENTRY = "{yb}d";
    private static final String INVALID_KEY_STRUCT = "(yb)d";
    private static final String INVALID_KEY_VARIANT = "vd";
    private static final String VALID_BYTE_VARIANT = "{yv}";

    private static Stream<Arguments> createInvalidKeySignatures() {
        return Stream.of(
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_ARRAY),
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_DICT_ENTRY),
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_STRUCT),
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_VARIANT),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_ARRAY),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_DICT_ENTRY),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_STRUCT),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_VARIANT)
        );
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeDictEntry(final ByteOrder byteOrder) throws DecoderException, SignatureException {
        final Signature signature = Signature.valueOf(VALID_BYTE_VARIANT);
        final ByteBuf buffer = Unpooled.buffer();
        // Byte (y)
        buffer.writeByte(Byte.MAX_VALUE);
        // Variant (v)
        final String rawSignature = "i";
        buffer.writeByte(rawSignature.length());
        buffer.writeBytes(rawSignature.getBytes(StandardCharsets.UTF_8));
        buffer.writeZero(1);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(Integer.MAX_VALUE);
        } else {
            buffer.writeIntLE(Integer.MAX_VALUE);
        }
        final DictEntryDecoder<DBusByte, Variant> decoder = new DictEntryDecoder<>(byteOrder, signature);
        final DecoderResult<DictEntry<DBusByte, Variant>> result = decoder.decode(buffer, 0);
        final int expectedBytes = 8;
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DictEntry<DBusByte, Variant> entry = result.getValue();
        assertEquals(Byte.MAX_VALUE, entry.getKey().getDelegate());
        final Int32 int32 = (Int32) entry.getValue().getDelegate();
        assertEquals(Integer.MAX_VALUE, int32.getDelegate());
    }

    @ParameterizedTest
    @MethodSource("createInvalidKeySignatures")
    void failDueToInvalidKey(final ByteOrder byteOrder, final String rawSignature)
            throws SignatureException {
        final Signature signature = Signature.valueOf(rawSignature);
        final ByteBuf buffer = Unpooled.buffer();
        assertThrows(DecoderException.class, () -> {
            final DictEntryDecoder<DBusByte, Variant> decoder = new DictEntryDecoder<>(byteOrder, signature);
            decoder.decode(buffer, 0);
        });
    }
}
