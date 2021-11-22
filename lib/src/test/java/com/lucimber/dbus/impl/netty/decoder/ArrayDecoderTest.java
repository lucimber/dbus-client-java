package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Int16;
import com.lucimber.dbus.type.Int64;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Struct;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ArrayDecoderTest {

    private static final String ARRAY_OF_BYTES = "ay";
    private static final String ARRAY_OF_BYTE_ARRAYS = "aay";
    private static final String ARRAY_OF_DOUBLES = "ad";
    private static final String ARRAY_OF_DOUBLE_ARRAYS = "aad";
    private static final String ARRAY_OF_SIGNED_LONGS = "ax";
    private static final String ARRAY_OF_SIGNED_SHORTS = "an";
    private static final String ARRAY_OF_STRINGS = "as";
    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
    private static final String ASSERT_SIZE_OF_ARRAY = "Size of array";
    private static final String HEADER_SIGNATURE = "a(yv)";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeArrayOfBytes(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_BYTES);
        final ByteBuf buffer = Unpooled.buffer();
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
        final int numOfBytes = buffer.readableBytes();
        final ArrayDecoder<DBusByte> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<DBusByte>> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<DBusByte> array = result.getValue();
        assertEquals(5, array.size(), ASSERT_SIZE_OF_ARRAY);
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeMessageHeader(final ByteOrder byteOrder) {
        final ByteBuf buffer = Unpooled.buffer();
        // Length of array
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(52);
        } else {
            buffer.writeIntLE(52);
        }
        buffer.writeZero(4); // Pad to struct boundary
        // Struct 1 (yv)
        buffer.writeByte(4);
        final byte[] v1Signature = "s".getBytes(StandardCharsets.UTF_8);
        buffer.writeByte(v1Signature.length);
        buffer.writeBytes(v1Signature);
        buffer.writeZero(1); // NUL byte
        final byte[] v1Content = "io.lucimber.Error.UnitTest".getBytes(StandardCharsets.UTF_8);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(v1Content.length);
        } else {
            buffer.writeIntLE(v1Content.length);
        }
        buffer.writeBytes(v1Content);
        buffer.writeZero(1); // NUL byte
        buffer.writeZero(5); // Pad to struct/variant boundary
        // Struct 2 (yv)
        buffer.writeByte(5);
        final byte[] v2Signature = "i".getBytes(StandardCharsets.UTF_8);
        buffer.writeByte(v2Signature.length);
        buffer.writeBytes(v2Signature);
        buffer.writeZero(1); // NUL byte
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(7);
        } else {
            buffer.writeIntLE(7);
        }
        // Test
        final int expectedBytes = 56;
        final Signature signature = Signature.valueOf(HEADER_SIGNATURE);
        final ArrayDecoder<Struct> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<Struct>> result = decoder.decode(buffer, 0);
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<Struct> array = result.getValue();
        assertEquals(2, array.size(), ASSERT_SIZE_OF_ARRAY);
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeArrayOfArrays(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_BYTE_ARRAYS);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(22);
        } else {
            buffer.writeIntLE(22);
        }
        // aa1
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(2);
        } else {
            buffer.writeIntLE(2);
        }
        buffer.writeByte(1);
        buffer.writeByte(2);
        buffer.writeZero(2); // Padding for array
        // aa2
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(2);
        } else {
            buffer.writeIntLE(2);
        }
        buffer.writeByte(1);
        buffer.writeByte(2);
        buffer.writeZero(2); // Padding for array
        // aa3
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(2);
        } else {
            buffer.writeIntLE(2);
        }
        buffer.writeByte(1);
        buffer.writeByte(2);
        final int expectedBytes = 26;
        final ArrayDecoder<DBusArray<DBusByte>> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<DBusArray<DBusByte>>> result = decoder.decode(buffer, 0);
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<DBusArray<DBusByte>> array = result.getValue();
        assertEquals(3, array.size(), ASSERT_SIZE_OF_ARRAY);
        assertEquals(2, array.get(0).size());
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeArrayOfDoubles(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_DOUBLES);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(3 * 8);
        } else {
            buffer.writeIntLE(3 * 8);
        }
        buffer.writeZero(4);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeDouble(1);
            buffer.writeDouble(2);
            buffer.writeDouble(3);
        } else {
            buffer.writeDoubleLE(1);
            buffer.writeDoubleLE(2);
            buffer.writeDoubleLE(3);
        }
        final int numOfBytes = buffer.readableBytes();
        final ArrayDecoder<DBusDouble> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<DBusDouble>> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<DBusDouble> array = result.getValue();
        assertEquals(3, array.size(), ASSERT_SIZE_OF_ARRAY);
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeArrayOfDoubleArrays(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_DOUBLE_ARRAYS);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(92);
        } else {
            buffer.writeIntLE(92);
        }
        // aa1
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(24);
        } else {
            buffer.writeIntLE(24);
        }
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeDouble(1);
            buffer.writeDouble(2);
            buffer.writeDouble(3);
        } else {
            buffer.writeDoubleLE(1);
            buffer.writeDoubleLE(2);
            buffer.writeDoubleLE(3);
        }
        // aa2
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(24);
        } else {
            buffer.writeIntLE(24);
        }
        buffer.writeZero(4);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeDouble(1);
            buffer.writeDouble(2);
            buffer.writeDouble(3);
        } else {
            buffer.writeDoubleLE(1);
            buffer.writeDoubleLE(2);
            buffer.writeDoubleLE(3);
        }
        // aa3
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(24);
        } else {
            buffer.writeIntLE(24);
        }
        buffer.writeZero(4);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeDouble(1);
            buffer.writeDouble(2);
            buffer.writeDouble(3);
        } else {
            buffer.writeDoubleLE(1);
            buffer.writeDoubleLE(2);
            buffer.writeDoubleLE(3);
        }
        final int numOfBytes = buffer.readableBytes();
        final ArrayDecoder<DBusArray<DBusDouble>> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<DBusArray<DBusDouble>>> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<DBusArray<DBusDouble>> array = result.getValue();
        assertEquals(3, array.size(), ASSERT_SIZE_OF_ARRAY);
        assertEquals(3, array.get(1).size(), "Sub list size");
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeArrayOfSignedShorts(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_SIGNED_SHORTS);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(4 * 2);
            buffer.writeShort(1);
            buffer.writeShort(2);
            buffer.writeShort(3);
            buffer.writeShort(4);
        } else {
            buffer.writeIntLE(4 * 2);
            buffer.writeShortLE(1);
            buffer.writeShortLE(2);
            buffer.writeShortLE(3);
            buffer.writeShortLE(4);
        }
        final int expectedBytes = buffer.readableBytes();
        final ArrayDecoder<Int16> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<Int16>> result = decoder.decode(buffer, 0);
        assertEquals(expectedBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<Int16> array = result.getValue();
        assertEquals(4, array.size(), ASSERT_SIZE_OF_ARRAY);
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeArrayOfStrings(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_STRINGS);
        final String s1 = "abc1";
        final String s2 = "def2";
        final byte[] s1bytes = s1.getBytes(StandardCharsets.UTF_8);
        final byte[] s2bytes = s2.getBytes(StandardCharsets.UTF_8);
        final ByteBuf stringBuffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            stringBuffer.writeInt(s1bytes.length);
        } else {
            stringBuffer.writeIntLE(s1bytes.length);
        }
        stringBuffer.writeBytes(s1bytes);
        stringBuffer.writeZero(1);
        stringBuffer.writeZero(3); // Padding of string
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            stringBuffer.writeInt(s2bytes.length);
        } else {
            stringBuffer.writeIntLE(s2bytes.length);
        }
        stringBuffer.writeBytes(s2bytes);
        stringBuffer.writeZero(1);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(stringBuffer.readableBytes());
        } else {
            buffer.writeIntLE(stringBuffer.readableBytes());
        }
        buffer.writeBytes(stringBuffer);
        final int numOfBytes = buffer.readableBytes();
        final ArrayDecoder<DBusString> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<DBusString>> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<DBusString> array = result.getValue();
        assertEquals(2, array.size(), ASSERT_SIZE_OF_ARRAY);
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void decodeEmptyArrayOfSignedLongs(final ByteOrder byteOrder) {
        final Signature signature = Signature.valueOf(ARRAY_OF_SIGNED_LONGS);
        final ByteBuf buffer = Unpooled.buffer();
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            buffer.writeInt(0);
        } else {
            buffer.writeIntLE(0);
        }
        buffer.writeZero(4);
        final int numOfBytes = buffer.readableBytes();
        final ArrayDecoder<Int64> decoder = new ArrayDecoder<>(byteOrder, signature);
        final DecoderResult<DBusArray<Int64>> result = decoder.decode(buffer, 0);
        assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
        final DBusArray<Int64> array = result.getValue();
        assertTrue(array.isEmpty(), ASSERT_SIZE_OF_ARRAY);
    }
}
