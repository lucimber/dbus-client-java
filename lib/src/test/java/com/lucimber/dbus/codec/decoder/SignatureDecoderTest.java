/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.lucimber.dbus.type.DBusSignature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SignatureDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

    private static final String COMPLEX_VALID_SIGNATURE = "a(bi)aaa{sb}uv(bh(ig))(qat)v";
    private static final String INVALID_BRACKET_SIGNATURE = "a(ia(ii)}";
    private static final String INVALID_CHAR_SIGNATURE = "z(i)";
    private static final String VALID_SIGNATURE = "a(ii)b";

    @Test
    void decodeValidSignature() {
        byte[] bytes = VALID_SIGNATURE.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + bytes.length + 1);
        buffer.put((byte) bytes.length);
        buffer.put(bytes);
        buffer.put((byte) 0x00);
        buffer.flip();

        SignatureDecoder decoder = new SignatureDecoder();
        DecoderResult<DBusSignature> result = decoder.decode(buffer, 0);

        assertEquals(buffer.limit(), result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
        assertEquals(VALID_SIGNATURE, result.getValue().toString());
        assertEquals(2, result.getValue().getQuantity());
    }

    @Test
    void decodeComplexValidSignature() {
        byte[] bytes = COMPLEX_VALID_SIGNATURE.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + bytes.length + 1);
        buffer.put((byte) bytes.length);
        buffer.put(bytes);
        buffer.put((byte) 0x00);
        buffer.flip();

        SignatureDecoder decoder = new SignatureDecoder();
        DecoderResult<DBusSignature> result = decoder.decode(buffer, 0);

        assertEquals(buffer.limit(), result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
        assertEquals(COMPLEX_VALID_SIGNATURE, result.getValue().toString());
        assertEquals(7, result.getValue().getQuantity());
    }

    @Test
    void decodeInvalidCharSignature() {
        byte[] bytes = INVALID_CHAR_SIGNATURE.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + bytes.length + 1);
        buffer.put((byte) bytes.length);
        buffer.put(bytes);
        buffer.put((byte) 0x00);
        buffer.flip();

        SignatureDecoder decoder = new SignatureDecoder();
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }

    @Test
    void decodeInvalidBracketSignature() {
        byte[] bytes = INVALID_BRACKET_SIGNATURE.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + bytes.length + 1);
        buffer.put((byte) bytes.length);
        buffer.put(bytes);
        buffer.put((byte) 0x00);
        buffer.flip();

        SignatureDecoder decoder = new SignatureDecoder();
        assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
    }
}
