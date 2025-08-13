/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.type.DBusInt16;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class ShortDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void decodeSignedShort(ByteOrder byteOrder) {
        ByteBuffer buffer = ByteBuffer.allocate(2).order(byteOrder);
        short value = (short) 0x8000; // -32768
        buffer.putShort(value);
        buffer.flip();

        Int16Decoder decoder = new Int16Decoder();
        DecoderResult<DBusInt16> result = decoder.decode(buffer, 0);

        assertEquals(2, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
        assertEquals(value, result.getValue().getDelegate());
    }
}
