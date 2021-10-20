package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.ObjectPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class StringEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSimpleString(final ByteOrder byteOrder) {
        final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final DBusString dbusString = DBusString.valueOf("abcABC_äüö");
        final EncoderResult<ByteBuf> result = encoder.encode(dbusString, 0);
        final int expectedBytes = 18;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        // UINT32 bytes
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x0D, buffer.readByte(), "Big Endian");
        } else {
            assertEquals((byte) 0x0D, buffer.readByte(), "Little Endian");
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
        }
        // UTF-8 bytes (13)
        buffer.skipBytes(13);
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeSimpleStringWithOffset(final ByteOrder byteOrder) {
        final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final DBusString dbusString = DBusString.valueOf("abcABC_äüö");
        final int offset = 5;
        final EncoderResult<ByteBuf> result = encoder.encode(dbusString, offset);
        final int expectedBytes = 21;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        buffer.skipBytes(3);
        // UINT32 bytes
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x0D, buffer.readByte(), "Big Endian");
        } else {
            assertEquals((byte) 0x0D, buffer.readByte(), "Little Endian");
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
        }
        // UTF-8 bytes (13)
        buffer.skipBytes(13);
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeEmptyString(final ByteOrder byteOrder) {
        final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final DBusString dbusString = DBusString.valueOf("");
        final EncoderResult<ByteBuf> result = encoder.encode(dbusString, 0);
        final int expectedBytes = 5;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        // UINT32
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte(), "Big Endian");
        } else {
            assertEquals((byte) 0x00, buffer.readByte(), "Little Endian");
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
        }
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
        buffer.release();
    }

    @ParameterizedTest
    @EnumSource(ByteOrder.class)
    void encodeStringOfObjectPath(final ByteOrder byteOrder) {
        final ObjectPath objectPath = ObjectPath.valueOf("/abc/d1/e_f");
        final DBusString rawPath = DBusString.valueOf(objectPath.getWrappedValue().toString());
        final Encoder<DBusString, ByteBuf> encoder = new StringEncoder(ByteBufAllocator.DEFAULT, byteOrder);
        final EncoderResult<ByteBuf> result = encoder.encode(rawPath, 0);
        final int expectedBytes = 16;
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        final ByteBuf buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
        // UINT32 bytes
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x0B, buffer.readByte(), "Big Endian");
        } else {
            assertEquals((byte) 0x0B, buffer.readByte(), "Little Endian");
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
            assertEquals((byte) 0x00, buffer.readByte());
        }
        // UTF-8 bytes (11 bytes)
        buffer.skipBytes(11);
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.readByte(), "Trailing NUL byte");
        buffer.release();
    }
}
