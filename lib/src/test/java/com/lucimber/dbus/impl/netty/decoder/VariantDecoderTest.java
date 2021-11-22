package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.TypeCode;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class VariantDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
    private static final String BYTE_SIGNATURE = "y";
    private static final String DOUBLE_SIGNATURE = "d";
    private static final String INTEGER_SIGNATURE = "i";
    private static final String TOO_MANY_TYPES = "ii";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeVariantOfByte(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(BYTE_SIGNATURE.length());
        buffer.writeBytes(BYTE_SIGNATURE.getBytes(StandardCharsets.UTF_8));
        buffer.writeZero(1); // NUL byte
        buffer.writeByte(Byte.MAX_VALUE);
        final int expectedBytes = 4;
        final VariantDecoder decoder = new VariantDecoder(byteOrder);
        final DecoderResult<Variant> result = decoder.decode(buffer, 0);
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Variant variant = result.getValue();
        assertEquals(TypeCode.BYTE, variant.getDelegate().getType().getCode());
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeVariantOfDouble(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(DOUBLE_SIGNATURE.length());
        buffer.writeBytes(DOUBLE_SIGNATURE.getBytes(StandardCharsets.UTF_8));
        buffer.writeZero(1); // NUL byte
        buffer.writeZero(5); // Padding for double
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeDouble(13.7);
        } else {
            buffer.writeDoubleLE(13.7);
        }
        final int numOfBytes = buffer.readableBytes();
        final VariantDecoder decoder = new VariantDecoder(byteOrder);
        final DecoderResult<Variant> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Variant variant = result.getValue();
        assertEquals(TypeCode.DOUBLE, variant.getDelegate().getType().getCode());
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeVariantOfInteger(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(INTEGER_SIGNATURE.length());
        buffer.writeBytes(INTEGER_SIGNATURE.getBytes(StandardCharsets.UTF_8));
        buffer.writeZero(1); // NUL byte
        buffer.writeZero(1); // Pad for INT32
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(Integer.MAX_VALUE);
        } else {
            buffer.writeIntLE(Integer.MAX_VALUE);
        }
        final int expectedBytes = 8;
        final VariantDecoder decoder = new VariantDecoder(ByteOrder.BIG_ENDIAN);
        final DecoderResult<Variant> result = decoder.decode(buffer, 0);
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final Variant variant = result.getValue();
        assertEquals(TypeCode.INT32, variant.getDelegate().getType().getCode());
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void failDueToTooManyTypes(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(TOO_MANY_TYPES.length());
        buffer.writeBytes(TOO_MANY_TYPES.getBytes(StandardCharsets.UTF_8));
        buffer.writeZero(1);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(1024);
            buffer.writeInt(2048);
        } else {
            buffer.writeIntLE(1024);
            buffer.writeIntLE(2048);
        }
        final VariantDecoder decoder = new VariantDecoder(byteOrder);
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }
}
