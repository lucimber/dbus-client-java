/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.lucimber.dbus.type.DBusUnixFD;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class UnixFdEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnixFdZero(ByteOrder byteOrder) {
        Encoder<DBusUnixFD, ByteBuffer> encoder = new UnixFdEncoder(byteOrder);
        DBusUnixFD unixFd = DBusUnixFD.valueOf(0);
        EncoderResult<ByteBuffer> result = encoder.encode(unixFd, 0);
        int expectedNumOfBytes = 4;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);

        byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00};
        byte[] actualBytes = new byte[expectedNumOfBytes];
        buffer.get(0, actualBytes);
        assertArrayEquals(expectedBytes, actualBytes, byteOrder + " byte order");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnixFdStandardValues(ByteOrder byteOrder) {
        Encoder<DBusUnixFD, ByteBuffer> encoder = new UnixFdEncoder(byteOrder);

        // Test with standard stdin (0)
        DBusUnixFD stdin = DBusUnixFD.valueOf(0);
        EncoderResult<ByteBuffer> stdinResult = encoder.encode(stdin, 0);
        assertEquals(4, stdinResult.getProducedBytes(), "stdin: " + PRODUCED_BYTES);
        byte[] stdinExpected = {0x00, 0x00, 0x00, 0x00};
        byte[] stdinActual = new byte[4];
        stdinResult.getBuffer().get(0, stdinActual);
        assertArrayEquals(stdinExpected, stdinActual, byteOrder + " stdin");

        // Test with standard stdout (1)
        DBusUnixFD stdout = DBusUnixFD.valueOf(1);
        EncoderResult<ByteBuffer> stdoutResult = encoder.encode(stdout, 0);
        assertEquals(4, stdoutResult.getProducedBytes(), "stdout: " + PRODUCED_BYTES);
        byte[] stdoutExpected, stdoutActual = new byte[4];
        stdoutResult.getBuffer().get(0, stdoutActual);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            stdoutExpected = new byte[] {0x00, 0x00, 0x00, 0x01};
        } else {
            stdoutExpected = new byte[] {0x01, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(stdoutExpected, stdoutActual, byteOrder + " stdout");

        // Test with standard stderr (2)
        DBusUnixFD stderr = DBusUnixFD.valueOf(2);
        EncoderResult<ByteBuffer> stderrResult = encoder.encode(stderr, 0);
        assertEquals(4, stderrResult.getProducedBytes(), "stderr: " + PRODUCED_BYTES);
        byte[] stderrExpected, stderrActual = new byte[4];
        stderrResult.getBuffer().get(0, stderrActual);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            stderrExpected = new byte[] {0x00, 0x00, 0x00, 0x02};
        } else {
            stderrExpected = new byte[] {0x02, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(stderrExpected, stderrActual, byteOrder + " stderr");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnixFdWithMultipleOffsets(ByteOrder byteOrder) {
        DBusUnixFD unixFd = DBusUnixFD.valueOf(42);
        Encoder<DBusUnixFD, ByteBuffer> encoder = new UnixFdEncoder(byteOrder);

        // Test with offset 0 - no padding needed
        EncoderResult<ByteBuffer> result0 = encoder.encode(unixFd, 0);
        assertEquals(4, result0.getProducedBytes(), "Offset 0: " + PRODUCED_BYTES);
        byte[] expected0, actual0 = new byte[4];
        result0.getBuffer().get(0, actual0);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected0 = new byte[] {0x00, 0x00, 0x00, 0x2A};
        } else {
            expected0 = new byte[] {0x2A, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected0, actual0, byteOrder + " Offset 0");

        // Test with offset 1 - requires 3 bytes padding to reach 4-byte boundary
        EncoderResult<ByteBuffer> result1 = encoder.encode(unixFd, 1);
        assertEquals(7, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);
        byte[] expected1, actual1 = new byte[7];
        result1.getBuffer().get(0, actual1);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected1 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2A};
        } else {
            expected1 = new byte[] {0x00, 0x00, 0x00, 0x2A, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected1, actual1, byteOrder + " Offset 1");

        // Test with offset 2 - requires 2 bytes padding to reach 4-byte boundary
        EncoderResult<ByteBuffer> result2 = encoder.encode(unixFd, 2);
        assertEquals(6, result2.getProducedBytes(), "Offset 2: " + PRODUCED_BYTES);
        byte[] expected2, actual2 = new byte[6];
        result2.getBuffer().get(0, actual2);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected2 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x2A};
        } else {
            expected2 = new byte[] {0x00, 0x00, 0x2A, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected2, actual2, byteOrder + " Offset 2");

        // Test with offset 3 - requires 1 byte padding to reach 4-byte boundary
        EncoderResult<ByteBuffer> result3 = encoder.encode(unixFd, 3);
        assertEquals(5, result3.getProducedBytes(), "Offset 3: " + PRODUCED_BYTES);
        byte[] expected3, actual3 = new byte[5];
        result3.getBuffer().get(0, actual3);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected3 = new byte[] {0x00, 0x00, 0x00, 0x00, 0x2A};
        } else {
            expected3 = new byte[] {0x00, 0x2A, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected3, actual3, byteOrder + " Offset 3");

        // Test with offset 4 - no padding needed, already at 4-byte boundary
        EncoderResult<ByteBuffer> result4 = encoder.encode(unixFd, 4);
        assertEquals(4, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);
        byte[] expected4, actual4 = new byte[4];
        result4.getBuffer().get(0, actual4);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected4 = new byte[] {0x00, 0x00, 0x00, 0x2A};
        } else {
            expected4 = new byte[] {0x2A, 0x00, 0x00, 0x00};
        }
        assertArrayEquals(expected4, actual4, byteOrder + " Offset 4");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnixFdTypicalValues(ByteOrder byteOrder) {
        Encoder<DBusUnixFD, ByteBuffer> encoder = new UnixFdEncoder(byteOrder);

        // Test with typical file descriptor values
        int[] fdValues = {3, 4, 5, 10, 20, 42, 100, 1000, 65535};

        for (int fdValue : fdValues) {
            DBusUnixFD unixFd = DBusUnixFD.valueOf(fdValue);
            EncoderResult<ByteBuffer> result = encoder.encode(unixFd, 0);
            assertEquals(4, result.getProducedBytes(), "FD " + fdValue + ": " + PRODUCED_BYTES);

            byte[] expected, actual = new byte[4];
            result.getBuffer().get(0, actual);
            if (byteOrder == ByteOrder.BIG_ENDIAN) {
                expected =
                        new byte[] {
                            (byte) ((fdValue >>> 24) & 0xFF),
                            (byte) ((fdValue >>> 16) & 0xFF),
                            (byte) ((fdValue >>> 8) & 0xFF),
                            (byte) (fdValue & 0xFF)
                        };
            } else {
                expected =
                        new byte[] {
                            (byte) (fdValue & 0xFF),
                            (byte) ((fdValue >>> 8) & 0xFF),
                            (byte) ((fdValue >>> 16) & 0xFF),
                            (byte) ((fdValue >>> 24) & 0xFF)
                        };
            }
            assertArrayEquals(expected, actual, byteOrder + " FD " + fdValue);
        }
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnixFdMaxValue(ByteOrder byteOrder) {
        Encoder<DBusUnixFD, ByteBuffer> encoder = new UnixFdEncoder(byteOrder);

        // Test with maximum 32-bit signed integer value
        DBusUnixFD maxFd = DBusUnixFD.valueOf(Integer.MAX_VALUE);
        EncoderResult<ByteBuffer> result = encoder.encode(maxFd, 0);
        assertEquals(4, result.getProducedBytes(), "Max FD: " + PRODUCED_BYTES);

        byte[] expected, actual = new byte[4];
        result.getBuffer().get(0, actual);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected = new byte[] {0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        } else {
            expected = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F};
        }
        assertArrayEquals(expected, actual, byteOrder + " Max FD");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnixFdNegativeValue(ByteOrder byteOrder) {
        Encoder<DBusUnixFD, ByteBuffer> encoder = new UnixFdEncoder(byteOrder);

        // Test with -1 (often used to indicate invalid FD)
        DBusUnixFD invalidFd = DBusUnixFD.valueOf(-1);
        EncoderResult<ByteBuffer> result = encoder.encode(invalidFd, 0);
        assertEquals(4, result.getProducedBytes(), "Invalid FD: " + PRODUCED_BYTES);

        byte[] expected, actual = new byte[4];
        result.getBuffer().get(0, actual);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            expected = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        } else {
            expected = new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        }
        assertArrayEquals(expected, actual, byteOrder + " Invalid FD");
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeUnixFdPowerOfTwoValues(ByteOrder byteOrder) {
        Encoder<DBusUnixFD, ByteBuffer> encoder = new UnixFdEncoder(byteOrder);

        // Test with powers of 2
        int[] powerOfTwoValues = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536
        };

        for (int fdValue : powerOfTwoValues) {
            DBusUnixFD unixFd = DBusUnixFD.valueOf(fdValue);
            EncoderResult<ByteBuffer> result = encoder.encode(unixFd, 0);
            assertEquals(4, result.getProducedBytes(), "FD " + fdValue + ": " + PRODUCED_BYTES);

            byte[] expected, actual = new byte[4];
            result.getBuffer().get(0, actual);
            if (byteOrder == ByteOrder.BIG_ENDIAN) {
                expected =
                        new byte[] {
                            (byte) ((fdValue >>> 24) & 0xFF),
                            (byte) ((fdValue >>> 16) & 0xFF),
                            (byte) ((fdValue >>> 8) & 0xFF),
                            (byte) (fdValue & 0xFF)
                        };
            } else {
                expected =
                        new byte[] {
                            (byte) (fdValue & 0xFF),
                            (byte) ((fdValue >>> 8) & 0xFF),
                            (byte) ((fdValue >>> 16) & 0xFF),
                            (byte) ((fdValue >>> 24) & 0xFF)
                        };
            }
            assertArrayEquals(expected, actual, byteOrder + " FD " + fdValue);
        }
    }
}
