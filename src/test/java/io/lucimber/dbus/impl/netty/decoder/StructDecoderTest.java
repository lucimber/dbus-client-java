package io.lucimber.dbus.impl.netty.decoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.Struct;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class StructDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
    private static final String FIRST_COMPLEX_SIGNATURE = "(ayv)";
    private static final String ONE_BYTE_SIGNATURE = "(y)";
    private static final String ONE_DOUBLE_SIGNATURE = "(d)";
    private static final String ONE_INTEGER_SIGNATURE = "(i)";
    private static final String TWO_BYTES_SIGNATURE = "(yy)";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeFirstComplexSignature(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(FIRST_COMPLEX_SIGNATURE);
        final ByteBuf buffer = Unpooled.buffer();
        // Array of bytes
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(5);
        } else {
            buffer.writeIntLE(5);
        }
        buffer.writeByte(1);
        buffer.writeByte(2);
        buffer.writeByte(3);
        buffer.writeByte(4);
        buffer.writeByte(5);
        // Variant
        final String rawVariantSignature = "i";
        buffer.writeByte(rawVariantSignature.length());
        buffer.writeBytes(rawVariantSignature.getBytes(StandardCharsets.UTF_8));
        buffer.writeZero(1);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(Integer.MAX_VALUE);
        } else {
            buffer.writeIntLE(Integer.MAX_VALUE);
        }
        final int expectedBytes = 16;
        final StructDecoder decoder = new StructDecoder(byteOrder, signature);
        final DecoderResult<Struct> result = decoder.decode(buffer, 0);
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Struct struct = result.getValue();
        assertEquals(FIRST_COMPLEX_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
        assertEquals(2, struct.getDelegate().size(), "Elements in struct");
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeStructOfOneByte(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ONE_BYTE_SIGNATURE);
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(Byte.MAX_VALUE);
        final StructDecoder decoder = new StructDecoder(byteOrder, signature);
        final DecoderResult<Struct> result = decoder.decode(buffer, 0);
        assertEquals(1, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Struct struct = result.getValue();
        assertEquals(ONE_BYTE_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
        assertEquals(1, struct.getDelegate().size(), "Elements in struct");
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeStructOfTwoBytes(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(TWO_BYTES_SIGNATURE);
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(Byte.MAX_VALUE);
        buffer.writeByte(Byte.MIN_VALUE);
        final StructDecoder decoder = new StructDecoder(byteOrder, signature);
        final DecoderResult<Struct> result = decoder.decode(buffer, 0);
        assertEquals(2, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Struct struct = result.getValue();
        assertEquals(TWO_BYTES_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
        assertEquals(2, struct.getDelegate().size(), "Elements in struct");
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeStructOfOneDouble(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ONE_DOUBLE_SIGNATURE);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeDouble(Double.MAX_VALUE);
        } else {
            buffer.writeDoubleLE(Double.MAX_VALUE);
        }
        final StructDecoder decoder = new StructDecoder(byteOrder, signature);
        final DecoderResult<Struct> result = decoder.decode(buffer, 0);
        assertEquals(8, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Struct struct = result.getValue();
        assertEquals(ONE_DOUBLE_SIGNATURE, struct.getSignature().getDelegate(), "Signature");
        assertEquals(1, struct.getDelegate().size(), "Elements in struct");
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodingStructOfOneInteger(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ONE_INTEGER_SIGNATURE);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(Integer.MAX_VALUE);
        } else {
            buffer.writeIntLE(Integer.MAX_VALUE);
        }
        final StructDecoder decoder = new StructDecoder(byteOrder, signature);
        final DecoderResult<Struct> result = decoder.decode(buffer, 0);
        assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Struct struct = result.getValue();
        assertEquals(ONE_INTEGER_SIGNATURE, struct.getSignature().toString(), "Signature");
        assertEquals(1, struct.getDelegate().size(), "Elements in struct");
    }
}
