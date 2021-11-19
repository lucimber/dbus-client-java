package io.lucimber.dbus.impl.netty.decoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusByte;
import io.lucimber.dbus.type.Dict;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DictionaryDecoderTest {

    private static final String ARRAY_OF_ENTRIES = "a{yv}";
    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeDictionary(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_ENTRIES);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(12);
        } else {
            buffer.writeIntLE(12);
        }
        buffer.writeZero(4); // Padding for dict-entry
        // Byte (y)
        buffer.writeByte(Byte.MAX_VALUE);
        // Variant (v)
        final String rawSignature = "i";
        buffer.writeByte(rawSignature.length());
        buffer.writeBytes(rawSignature.getBytes(StandardCharsets.UTF_8));
        buffer.writeZero(1); // NUL byte
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(Integer.MAX_VALUE);
        } else {
            buffer.writeIntLE(Integer.MAX_VALUE);
        }
        final int expectedBytes = 16;
        final DictDecoder<DBusByte, Variant> decoder = new DictDecoder<>(byteOrder, signature);
        final DecoderResult<Dict<DBusByte, Variant>> result = decoder.decode(buffer, 0);
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Dict<DBusByte, Variant> dict = result.getValue();
        assertEquals(1, dict.size());
        assertTrue(dict.containsKey(DBusByte.valueOf(Byte.MAX_VALUE)));
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeEmptyDictionary(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_ENTRIES);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(0);
        } else {
            buffer.writeIntLE(0);
        }
        buffer.writeZero(4);
        final int numOfBytes = buffer.readableBytes();
        final DictDecoder<DBusByte, Variant> decoder = new DictDecoder<>(byteOrder, signature);
        final DecoderResult<Dict<DBusByte, Variant>> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Dict<DBusByte, Variant> dict = result.getValue();
        assertTrue(dict.isEmpty());
        buffer.release();
    }
}
