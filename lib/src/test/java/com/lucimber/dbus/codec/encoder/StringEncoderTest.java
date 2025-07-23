/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class StringEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeSimpleString(ByteOrder byteOrder) {
        Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
        DBusString dbusString = DBusString.valueOf("abcABC_äüö");
        EncoderResult<ByteBuffer> result = encoder.encode(dbusString, 0);
        int expectedNumOfBytes = 18;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        // UINT32 bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0D};
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
        } else {
            byte[] expectedBytes = {0x0D, 0x00, 0x00, 0x00};
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
        }
        // UINT32 bytes (4) + UTF-8 bytes (13)
        int numOfRemainingBytes = 17;
        buffer.position(numOfRemainingBytes);
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeSimpleStringWithOffset(ByteOrder byteOrder) {
        Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
        DBusString dbusString = DBusString.valueOf("abcABC_äüö");
        int offset = 5;
        EncoderResult<ByteBuffer> result = encoder.encode(dbusString, offset);
        int expectedNumOfBytes = 21;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        int numOfOffsetBytes = 3;
        buffer.position(numOfOffsetBytes);
        // UINT32 bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0D};
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
        } else {
            byte[] expectedBytes = {0x0D, 0x00, 0x00, 0x00};
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
        }
        // 3 + UINT32 bytes (4) + UTF-8 bytes (13)
        int numOfRemainingBytes = 20;
        buffer.position(numOfRemainingBytes);
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeEmptyString(ByteOrder byteOrder) {
        Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
        DBusString dbusString = DBusString.valueOf("");
        EncoderResult<ByteBuffer> result = encoder.encode(dbusString, 0);
        int expectedNumOfBytes = 5;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        // UINT32
        int numOfLengthBytes = 4;
        byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00};
        byte[] actualBytes = new byte[numOfLengthBytes];
        buffer.get(actualBytes, 0, numOfLengthBytes);
        assertArrayEquals(expectedBytes, actualBytes);
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeStringOfObjectPath(ByteOrder byteOrder) {
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/abc/d1/e_f");
        DBusString rawPath = DBusString.valueOf(objectPath.getWrappedValue().toString());
        Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
        EncoderResult<ByteBuffer> result = encoder.encode(rawPath, 0);
        int expectedNumOfBytes = 16;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        // UINT32 bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x0B};
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian");
        } else {
            byte[] expectedBytes = {0x0B, 0x00, 0x00, 0x00};
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian");
        }
        // UINT32 bytes (4) + UTF-8 bytes (11 bytes)
        int numOfRemainingBytes = 15;
        buffer.position(numOfRemainingBytes);
        // Trailing NUL byte
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeStringWithMultipleOffsets(ByteOrder byteOrder) {
        Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
        DBusString dbusString = DBusString.valueOf("test");

        // Test with offset 1 (requires 3 bytes padding to reach 4-byte boundary)
        EncoderResult<ByteBuffer> result1 = encoder.encode(dbusString, 1);
        int expectedBytes1 = 12; // 3 padding + 4 length + 4 content + 1 NUL
        assertEquals(expectedBytes1, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);

        // Test with offset 2 (requires 2 bytes padding to reach 4-byte boundary)
        EncoderResult<ByteBuffer> result2 = encoder.encode(dbusString, 2);
        int expectedBytes2 = 11; // 2 padding + 4 length + 4 content + 1 NUL
        assertEquals(expectedBytes2, result2.getProducedBytes(), "Offset 2: " + PRODUCED_BYTES);

        // Test with offset 3 (requires 1 byte padding to reach 4-byte boundary)
        EncoderResult<ByteBuffer> result3 = encoder.encode(dbusString, 3);
        int expectedBytes3 = 10; // 1 padding + 4 length + 4 content + 1 NUL
        assertEquals(expectedBytes3, result3.getProducedBytes(), "Offset 3: " + PRODUCED_BYTES);

        // Test with offset 4 (no padding needed, already at 4-byte boundary)
        EncoderResult<ByteBuffer> result4 = encoder.encode(dbusString, 4);
        int expectedBytes4 = 9; // 0 padding + 4 length + 4 content + 1 NUL
        assertEquals(expectedBytes4, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);

        // Test with offset 7 (requires 1 byte padding to reach 4-byte boundary)
        EncoderResult<ByteBuffer> result7 = encoder.encode(dbusString, 7);
        int expectedBytes7 = 10; // 1 padding + 4 length + 4 content + 1 NUL
        assertEquals(expectedBytes7, result7.getProducedBytes(), "Offset 7: " + PRODUCED_BYTES);

        // Test with offset 8 (no padding needed, already at 4-byte boundary)
        EncoderResult<ByteBuffer> result8 = encoder.encode(dbusString, 8);
        int expectedBytes8 = 9; // 0 padding + 4 length + 4 content + 1 NUL
        assertEquals(expectedBytes8, result8.getProducedBytes(), "Offset 8: " + PRODUCED_BYTES);
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeEmptyStringWithMultipleOffsets(ByteOrder byteOrder) {
        Encoder<DBusString, ByteBuffer> encoder = new StringEncoder(byteOrder);
        DBusString emptyString = DBusString.valueOf("");

        // Test with various offsets for empty string
        for (int offset = 0; offset < 8; offset++) {
            EncoderResult<ByteBuffer> result = encoder.encode(emptyString, offset);

            // Calculate expected padding to reach 4-byte boundary
            int paddingNeeded = (4 - (offset % 4)) % 4;
            int expectedBytes = paddingNeeded + 4 + 1; // padding + length + NUL

            assertEquals(
                    expectedBytes,
                    result.getProducedBytes(),
                    "Offset " + offset + ": " + PRODUCED_BYTES);

            ByteBuffer buffer = result.getBuffer();
            assertEquals(
                    expectedBytes, buffer.remaining(), "Offset " + offset + ": " + READABLE_BYTES);
        }
    }
}
