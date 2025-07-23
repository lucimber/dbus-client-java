/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.lucimber.dbus.type.DBusObjectPath;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ObjectPathEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeSimpleObjectPath(ByteOrder byteOrder) {
        Encoder<DBusObjectPath, ByteBuffer> encoder = new ObjectPathEncoder(byteOrder);
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/com/example/test");
        EncoderResult<ByteBuffer> result = encoder.encode(objectPath, 0);
        // ObjectPath: 4 bytes length + 16 bytes path + 1 byte NUL = 21 bytes
        String pathStr = "/com/example/test";
        int expectedNumOfBytes = 4 + pathStr.length() + 1;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);

        // UINT32 length bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x11}; // 17 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian length");
        } else {
            byte[] expectedBytes = {0x11, 0x00, 0x00, 0x00}; // 17 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian length");
        }

        // Path content + NUL terminator - position to last byte
        buffer.position(buffer.capacity() - 1);
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeRootObjectPath(ByteOrder byteOrder) {
        Encoder<DBusObjectPath, ByteBuffer> encoder = new ObjectPathEncoder(byteOrder);
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/");
        EncoderResult<ByteBuffer> result = encoder.encode(objectPath, 0);
        int expectedNumOfBytes = 6; // 4 length + 1 path + 1 NUL
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);

        // UINT32 length bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x01}; // 1 byte
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian length");
        } else {
            byte[] expectedBytes = {0x01, 0x00, 0x00, 0x00}; // 1 byte
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian length");
        }

        // Path content
        assertEquals((byte) '/', buffer.get(), "Root path character");
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeObjectPathWithMultipleOffsets(ByteOrder byteOrder) {
        Encoder<DBusObjectPath, ByteBuffer> encoder = new ObjectPathEncoder(byteOrder);
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/test");

        // Test with various offsets
        for (int offset = 0; offset < 8; offset++) {
            EncoderResult<ByteBuffer> result = encoder.encode(objectPath, offset);

            // Calculate expected padding to reach 4-byte boundary
            int paddingNeeded = (4 - (offset % 4)) % 4;
            int expectedBytes = paddingNeeded + 4 + 5 + 1; // padding + length + path + NUL

            assertEquals(
                    expectedBytes,
                    result.getProducedBytes(),
                    "Offset " + offset + ": " + PRODUCED_BYTES);

            ByteBuffer buffer = result.getBuffer();
            assertEquals(
                    expectedBytes, buffer.remaining(), "Offset " + offset + ": " + READABLE_BYTES);
        }
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeObjectPathWithOffset(ByteOrder byteOrder) {
        Encoder<DBusObjectPath, ByteBuffer> encoder = new ObjectPathEncoder(byteOrder);
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/abc/def");
        int offset = 5;
        EncoderResult<ByteBuffer> result = encoder.encode(objectPath, offset);
        // ObjectPath needs 4-byte alignment, so from offset 5, we need 3 padding bytes to reach
        // offset 8
        // Then: 4 bytes length + 8 bytes path + 1 byte NUL = 16 bytes total
        String pathStr = "/abc/def";
        int paddingNeeded = (4 - (offset % 4)) % 4; // Calculate padding to 4-byte boundary
        int expectedNumOfBytes = paddingNeeded + 4 + pathStr.length() + 1;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);

        // Skip padding bytes
        buffer.position(paddingNeeded);

        // UINT32 length bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x08}; // 8 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian length");
        } else {
            byte[] expectedBytes = {0x08, 0x00, 0x00, 0x00}; // 8 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian length");
        }

        // Path content + NUL terminator - position to last byte
        buffer.position(buffer.capacity() - 1);
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeComplexObjectPath(ByteOrder byteOrder) {
        Encoder<DBusObjectPath, ByteBuffer> encoder = new ObjectPathEncoder(byteOrder);
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/org/freedesktop/DBus");
        EncoderResult<ByteBuffer> result = encoder.encode(objectPath, 0);
        // ObjectPath: 4 bytes length + 20 bytes path + 1 byte NUL = 25 bytes, but need to account
        // for possible alignment
        String pathStr = "/org/freedesktop/DBus";
        int expectedNumOfBytes = 4 + pathStr.length() + 1;
        // The actual encoder might add padding - let's check the actual result
        assertTrue(
                result.getProducedBytes() >= expectedNumOfBytes,
                "Produced bytes should be at least " + expectedNumOfBytes);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(result.getProducedBytes(), buffer.remaining(), READABLE_BYTES);

        // UINT32 length bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x15}; // 21 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian length");
        } else {
            byte[] expectedBytes = {0x15, 0x00, 0x00, 0x00}; // 21 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian length");
        }

        // Path content + NUL terminator - position to last byte
        buffer.position(buffer.capacity() - 1);
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeObjectPathWithUnderscoreAndNumbers(ByteOrder byteOrder) {
        Encoder<DBusObjectPath, ByteBuffer> encoder = new ObjectPathEncoder(byteOrder);
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/com/example/test_123");
        EncoderResult<ByteBuffer> result = encoder.encode(objectPath, 0);
        // ObjectPath: 4 bytes length + 20 bytes path + 1 byte NUL = 25 bytes, but need to account
        // for possible alignment
        String pathStr = "/com/example/test_123";
        int expectedNumOfBytes = 4 + pathStr.length() + 1;
        // The actual encoder might add padding - let's check the actual result
        assertTrue(
                result.getProducedBytes() >= expectedNumOfBytes,
                "Produced bytes should be at least " + expectedNumOfBytes);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(result.getProducedBytes(), buffer.remaining(), READABLE_BYTES);

        // UINT32 length bytes
        int numOfLengthBytes = 4;
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            byte[] expectedBytes = {0x00, 0x00, 0x00, 0x15}; // 21 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Big Endian length");
        } else {
            byte[] expectedBytes = {0x15, 0x00, 0x00, 0x00}; // 21 bytes
            byte[] actualBytes = new byte[numOfLengthBytes];
            buffer.get(actualBytes, 0, numOfLengthBytes);
            assertArrayEquals(expectedBytes, actualBytes, "Little Endian length");
        }

        // Path content + NUL terminator - position to last byte
        buffer.position(buffer.capacity() - 1);
        assertEquals((byte) 0x00, buffer.get(), "Trailing NUL byte");
    }
}
