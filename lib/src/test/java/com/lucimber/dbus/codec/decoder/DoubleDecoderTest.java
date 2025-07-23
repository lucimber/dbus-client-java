/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.type.DBusDouble;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

final class DoubleDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @Test
    void decodeDoubleOnBigEndian() {
        double expected = 2.3;
        byte[] bytes = {0x40, 0x02, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66}; // 2.3 in BE IEEE-754
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);

        DoubleDecoder decoder = new DoubleDecoder();
        DecoderResult<DBusDouble> result = decoder.decode(buffer, 0);

        assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
        assertEquals(expected, result.getValue().getDelegate());
    }

    @Test
    void decodeDoubleOnLittleEndian() {
        double expected = 2.3;
        byte[] bytes = {0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x02, 0x40}; // 2.3 in LE IEEE-754
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        DoubleDecoder decoder = new DoubleDecoder();
        DecoderResult<DBusDouble> result = decoder.decode(buffer, 0);

        assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
        assertEquals(expected, result.getValue().getDelegate());
    }
}
