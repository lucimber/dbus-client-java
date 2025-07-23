/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.lucimber.dbus.type.DBusInt64;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LongDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void decodeSignedLong(ByteOrder byteOrder) {
        long value = -9223372036854775808L;
        ByteBuffer buffer = ByteBuffer.allocate(8).order(byteOrder);
        buffer.putLong(value);
        buffer.flip();

        Int64Decoder decoder = new Int64Decoder();
        DecoderResult<DBusInt64> result = decoder.decode(buffer, 0);

        assertEquals(8, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
        assertEquals(value, result.getValue().getDelegate());
    }
}
