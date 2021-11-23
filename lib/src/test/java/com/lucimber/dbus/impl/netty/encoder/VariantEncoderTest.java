package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.Int32;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.UInt16;
import com.lucimber.dbus.type.UInt64;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VariantEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeVariantOfSignedInteger(final ByteOrder byteOrder) {
        final Variant variant = Variant.valueOf(Int32.valueOf(Integer.MAX_VALUE));
        final Encoder<Variant, ByteBuf> encoder = new VariantEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final EncoderResult<ByteBuf> result = encoder.encode(variant, 0);
        final int expectedBytes = 8; // 3 byte + 1 byte + 4 byte
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeVariantOfSignedIntegerWithOffset(final ByteOrder byteOrder) {
        final Variant variant = Variant.valueOf(Int32.valueOf(Integer.MAX_VALUE));
        final Encoder<Variant, ByteBuf> encoder = new VariantEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(variant, offset);
        final int expectedBytes = 7; // 3 byte + 4 byte
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeVariantOfArray(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf("at");
        final DBusArray<UInt64> array = new DBusArray<>(signature);
        array.add(UInt64.valueOf(Long.MAX_VALUE));
        final Variant variant = Variant.valueOf(array);
        final Encoder<Variant, ByteBuf> encoder = new VariantEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final EncoderResult<ByteBuf> result = encoder.encode(variant, 0);
        final int expectedBytes = 16; // 4 byte + 4 byte + 8 byte
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodingVariantOfStruct(final ByteOrder byteOrder) {
        final Signature structSig = Signature.valueOf("(aqo)");
        final Signature arraySig = Signature.valueOf("aq");
        final DBusArray<UInt16> dBusArray = new DBusArray<>(arraySig);
        dBusArray.add(UInt16.valueOf(Short.MIN_VALUE));
        dBusArray.add(UInt16.valueOf(Short.MAX_VALUE));
        final ObjectPath objectPath = ObjectPath.valueOf("/junit_object_path");
        final Struct struct = new Struct(structSig, dBusArray, objectPath);
        final Variant variant = Variant.valueOf(struct);
        final Encoder<Variant, ByteBuf> encoder = new VariantEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final EncoderResult<ByteBuf> result = encoder.encode(variant, 0);
        final int expectedBytes = 39; // 3 byte + 5 byte + 8 byte + 0 byte + 23 byte
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        buffer.release();
    }
}
