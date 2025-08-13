/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.type.DBusDouble;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

final class DoubleEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @Test
    void encodeDoubleOnBigEndian() {
        Encoder<DBusDouble, ByteBuffer> encoder = new DoubleEncoder(ByteOrder.BIG_ENDIAN);
        DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        EncoderResult<ByteBuffer> result = encoder.encode(dbusDouble, 0);
        int expectedNumOfBytes = 8;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        byte[] expectedBytes = {0x40, 0x02, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66};
        byte[] actualBytes = new byte[expectedNumOfBytes];
        buffer.get(0, actualBytes);
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void encodeDoubleWithOffsetOnBigEndian() {
        Encoder<DBusDouble, ByteBuffer> encoder = new DoubleEncoder(ByteOrder.BIG_ENDIAN);
        DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        int offset = 5;
        EncoderResult<ByteBuffer> result = encoder.encode(dbusDouble, offset);
        int expectedNumOfBytes = 11;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        byte[] expectedBytes = {0x00, 0x00, 0x00, 0x40, 0x02, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66};
        byte[] actualBytes = new byte[expectedNumOfBytes];
        buffer.get(0, actualBytes);
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void encodeDoubleOnLittleEndian() {
        Encoder<DBusDouble, ByteBuffer> encoder = new DoubleEncoder(ByteOrder.LITTLE_ENDIAN);
        DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        EncoderResult<ByteBuffer> result = encoder.encode(dbusDouble, 0);
        int expectedNumOfBytes = 8;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        byte[] expectedBytes = {0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x02, 0x40};
        byte[] actualBytes = new byte[expectedNumOfBytes];
        buffer.get(0, actualBytes);
        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    void encodeDoubleWithOffsetOnLittleEndian() {
        Encoder<DBusDouble, ByteBuffer> encoder = new DoubleEncoder(ByteOrder.LITTLE_ENDIAN);
        DBusDouble dbusDouble = DBusDouble.valueOf(2.3);
        int offset = 5;
        EncoderResult<ByteBuffer> result = encoder.encode(dbusDouble, offset);
        int expectedNumOfBytes = 11;
        assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
        byte[] expectedBytes = {0x00, 0x00, 0x00, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x02, 0x40};
        byte[] actualBytes = new byte[expectedNumOfBytes];
        buffer.get(0, actualBytes);
        assertArrayEquals(expectedBytes, actualBytes);
    }
}
