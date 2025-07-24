/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.codec.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.type.DBusUInt64;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class UInt64EncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnsignedLongMaxValue(ByteOrder byteOrder) {
        Encoder<DBusUInt64, ByteBuffer> encoder = new UInt64Encoder(byteOrder);
        DBusUInt64 uint64 = DBusUInt64.valueOf(-1L); // 0xFFFFFFFFFFFFFFFF as unsigned
        EncoderResult<ByteBuffer> result = encoder.encode(uint64, 0);
        int expectedNumOfBytes = 8;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);

        byte[] expectedBytes =
                new byte[] {
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF,
                    (byte) 0xFF
                };
        byte[] actualBytes = new byte[expectedNumOfBytes];
        buffer.get(0, actualBytes);
        assertArrayEquals(expectedBytes, actualBytes, byteOrder + " byte order");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnsignedLongMinValue(ByteOrder byteOrder) {
        Encoder<DBusUInt64, ByteBuffer> encoder = new UInt64Encoder(byteOrder);
        DBusUInt64 uint64 = DBusUInt64.valueOf(0L);
        EncoderResult<ByteBuffer> result = encoder.encode(uint64, 0);
        int expectedNumOfBytes = 8;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);

        byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] actualBytes = new byte[expectedNumOfBytes];
        buffer.get(0, actualBytes);
        assertArrayEquals(expectedBytes, actualBytes, byteOrder + " byte order");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnsignedLongWithMultipleOffsets(ByteOrder byteOrder) {
        DBusUInt64 uint64 = DBusUInt64.valueOf(0x123456789ABCDEFL);
        Encoder<DBusUInt64, ByteBuffer> encoder = new UInt64Encoder(byteOrder);

        // Test with offset 0 - no padding needed
        EncoderResult<ByteBuffer> result0 = encoder.encode(uint64, 0);
        assertEquals(8, result0.getProducedBytes(), "Offset 0: " + PRODUCED_BYTES);
        byte[] expected0, actual0 = new byte[8];
        result0.getBuffer().get(0, actual0);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected0 =
                    new byte[] {
                        0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
                    };
        } else {
            expected0 =
                    new byte[] {
                        (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, (byte) 0x89, 0x67, 0x45, 0x23, 0x01
                    };
        }
        assertArrayEquals(expected0, actual0, byteOrder + " Offset 0");

        // Test with offset 1 - requires 7 bytes padding to reach 8-byte boundary
        EncoderResult<ByteBuffer> result1 = encoder.encode(uint64, 1);
        assertEquals(15, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);
        byte[] expected1, actual1 = new byte[15];
        result1.getBuffer().get(0, actual1);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected1 =
                    new byte[] {
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x01,
                        0x23,
                        0x45,
                        0x67,
                        (byte) 0x89,
                        (byte) 0xAB,
                        (byte) 0xCD,
                        (byte) 0xEF
                    };
        } else {
            expected1 =
                    new byte[] {
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        (byte) 0xEF,
                        (byte) 0xCD,
                        (byte) 0xAB,
                        (byte) 0x89,
                        0x67,
                        0x45,
                        0x23,
                        0x01
                    };
        }
        assertArrayEquals(expected1, actual1, byteOrder + " Offset 1");

        // Test with offset 4 - requires 4 bytes padding to reach 8-byte boundary
        EncoderResult<ByteBuffer> result4 = encoder.encode(uint64, 4);
        assertEquals(12, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);
        byte[] expected4, actual4 = new byte[12];
        result4.getBuffer().get(0, actual4);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected4 =
                    new byte[] {
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x01,
                        0x23,
                        0x45,
                        0x67,
                        (byte) 0x89,
                        (byte) 0xAB,
                        (byte) 0xCD,
                        (byte) 0xEF
                    };
        } else {
            expected4 =
                    new byte[] {
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        (byte) 0xEF,
                        (byte) 0xCD,
                        (byte) 0xAB,
                        (byte) 0x89,
                        0x67,
                        0x45,
                        0x23,
                        0x01
                    };
        }
        assertArrayEquals(expected4, actual4, byteOrder + " Offset 4");

        // Test with offset 8 - no padding needed, already at 8-byte boundary
        EncoderResult<ByteBuffer> result8 = encoder.encode(uint64, 8);
        assertEquals(8, result8.getProducedBytes(), "Offset 8: " + PRODUCED_BYTES);
        byte[] expected8, actual8 = new byte[8];
        result8.getBuffer().get(0, actual8);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected8 =
                    new byte[] {
                        0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
                    };
        } else {
            expected8 =
                    new byte[] {
                        (byte) 0xEF, (byte) 0xCD, (byte) 0xAB, (byte) 0x89, 0x67, 0x45, 0x23, 0x01
                    };
        }
        assertArrayEquals(expected8, actual8, byteOrder + " Offset 8");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnsignedLongBoundaryValues(ByteOrder byteOrder) {
        Encoder<DBusUInt64, ByteBuffer> encoder = new UInt64Encoder(byteOrder);

        // Test with value 1
        DBusUInt64 one = DBusUInt64.valueOf(1L);
        EncoderResult<ByteBuffer> result1 = encoder.encode(one, 0);
        assertEquals(8, result1.getProducedBytes(), "Value 1: " + PRODUCED_BYTES);
        byte[] expected1, actual1 = new byte[8];
        result1.getBuffer().get(0, actual1);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected1 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01};
        } else {
            expected1 = new byte[] {0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected1, actual1, byteOrder + " Value 1");

        // Test with value 4294967295 (0xFFFFFFFF)
        DBusUInt64 value4294967295 = DBusUInt64.valueOf(4294967295L);
        EncoderResult<ByteBuffer> result4294967295 = encoder.encode(value4294967295, 0);
        assertEquals(8, result4294967295.getProducedBytes(), "Value 4294967295: " + PRODUCED_BYTES);
        byte[] expected4294967295, actual4294967295 = new byte[8];
        result4294967295.getBuffer().get(0, actual4294967295);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected4294967295 =
                    new byte[] {
                        0x00, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
                    };
        } else {
            expected4294967295 =
                    new byte[] {
                        (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00
                    };
        }
        assertArrayEquals(expected4294967295, actual4294967295, byteOrder + " Value 4294967295");

        // Test with value Long.MAX_VALUE (0x7FFFFFFFFFFFFFFF)
        DBusUInt64 valueMaxLong = DBusUInt64.valueOf(Long.MAX_VALUE);
        EncoderResult<ByteBuffer> resultMaxLong = encoder.encode(valueMaxLong, 0);
        assertEquals(
                8, resultMaxLong.getProducedBytes(), "Value Long.MAX_VALUE: " + PRODUCED_BYTES);
        byte[] expectedMaxLong, actualMaxLong = new byte[8];
        resultMaxLong.getBuffer().get(0, actualMaxLong);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expectedMaxLong =
                    new byte[] {
                        0x7F,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF
                    };
        } else {
            expectedMaxLong =
                    new byte[] {
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        0x7F
                    };
        }
        assertArrayEquals(expectedMaxLong, actualMaxLong, byteOrder + " Value Long.MAX_VALUE");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnsignedLongHighValues(ByteOrder byteOrder) {
        Encoder<DBusUInt64, ByteBuffer> encoder = new UInt64Encoder(byteOrder);

        // Test with value 2^63 (Long.MIN_VALUE as signed = 2^63 as unsigned)
        DBusUInt64 value2pow63 = DBusUInt64.valueOf(Long.MIN_VALUE);
        EncoderResult<ByteBuffer> result2pow63 = encoder.encode(value2pow63, 0);
        assertEquals(8, result2pow63.getProducedBytes(), "Value 2^63: " + PRODUCED_BYTES);
        byte[] expected2pow63, actual2pow63 = new byte[8];
        result2pow63.getBuffer().get(0, actual2pow63);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected2pow63 = new byte[] {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        } else {
            expected2pow63 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x80};
        }
        assertArrayEquals(expected2pow63, actual2pow63, byteOrder + " Value 2^63");

        // Test with value 18446744073709551614 (appears as -2 in signed long)
        DBusUInt64 value18446744073709551614 = DBusUInt64.valueOf(-2L);
        EncoderResult<ByteBuffer> result18446744073709551614 =
                encoder.encode(value18446744073709551614, 0);
        assertEquals(
                8,
                result18446744073709551614.getProducedBytes(),
                "Value 18446744073709551614: " + PRODUCED_BYTES);
        byte[] expected18446744073709551614, actual18446744073709551614 = new byte[8];
        result18446744073709551614.getBuffer().get(0, actual18446744073709551614);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected18446744073709551614 =
                    new byte[] {
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFE
                    };
        } else {
            expected18446744073709551614 =
                    new byte[] {
                        (byte) 0xFE,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF,
                        (byte) 0xFF
                    };
        }
        assertArrayEquals(
                expected18446744073709551614,
                actual18446744073709551614,
                byteOrder + " Value 18446744073709551614");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnsignedLongPowerOfTwoValues(ByteOrder byteOrder) {
        Encoder<DBusUInt64, ByteBuffer> encoder = new UInt64Encoder(byteOrder);

        // Test with value 256 (2^8)
        DBusUInt64 value256 = DBusUInt64.valueOf(256L);
        EncoderResult<ByteBuffer> result256 = encoder.encode(value256, 0);
        assertEquals(8, result256.getProducedBytes(), "Value 256: " + PRODUCED_BYTES);
        byte[] expected256, actual256 = new byte[8];
        result256.getBuffer().get(0, actual256);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected256 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00};
        } else {
            expected256 = new byte[] {0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected256, actual256, byteOrder + " Value 256");

        // Test with value 65536 (2^16)
        DBusUInt64 value65536 = DBusUInt64.valueOf(65536L);
        EncoderResult<ByteBuffer> result65536 = encoder.encode(value65536, 0);
        assertEquals(8, result65536.getProducedBytes(), "Value 65536: " + PRODUCED_BYTES);
        byte[] expected65536, actual65536 = new byte[8];
        result65536.getBuffer().get(0, actual65536);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected65536 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00};
        } else {
            expected65536 = new byte[] {0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected65536, actual65536, byteOrder + " Value 65536");

        // Test with value 4294967296 (2^32)
        DBusUInt64 value4294967296 = DBusUInt64.valueOf(4294967296L);
        EncoderResult<ByteBuffer> result4294967296 = encoder.encode(value4294967296, 0);
        assertEquals(8, result4294967296.getProducedBytes(), "Value 4294967296: " + PRODUCED_BYTES);
        byte[] expected4294967296, actual4294967296 = new byte[8];
        result4294967296.getBuffer().get(0, actual4294967296);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected4294967296 = new byte[] {0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00};
        } else {
            expected4294967296 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected4294967296, actual4294967296, byteOrder + " Value 4294967296");
    }
}
