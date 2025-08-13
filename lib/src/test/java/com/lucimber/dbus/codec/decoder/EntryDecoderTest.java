/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lucimber.dbus.type.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class EntryDecoderTest {

    private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
    private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
    private static final String INVALID_KEY_ARRAY = "ad";
    private static final String INVALID_KEY_DICT_ENTRY = "{yb}d";
    private static final String INVALID_KEY_STRUCT = "(yb)d";
    private static final String INVALID_KEY_VARIANT = "vd";
    private static final String VALID_BYTE_VARIANT = "{yv}";

    private static Stream<Arguments> createInvalidKeySignatures() {
        return Stream.of(
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_ARRAY),
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_DICT_ENTRY),
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_STRUCT),
                Arguments.of(ByteOrder.BIG_ENDIAN, INVALID_KEY_VARIANT),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_ARRAY),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_DICT_ENTRY),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_STRUCT),
                Arguments.of(ByteOrder.LITTLE_ENDIAN, INVALID_KEY_VARIANT));
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void decodeDictEntry(ByteOrder byteOrder) throws DecoderException, SignatureException {
        DBusSignature signature = DBusSignature.valueOf(VALID_BYTE_VARIANT);
        String variantSignature = "i";
        byte[] sigBytes = variantSignature.getBytes(StandardCharsets.UTF_8);

        int entrySize = 1 + 1 + sigBytes.length + 1 + 4;
        ByteBuffer buffer = ByteBuffer.allocate(entrySize).order(byteOrder);

        buffer.put((byte) 0x7F); // key: BYTE
        buffer.put((byte) sigBytes.length); // signature length
        buffer.put(sigBytes); // signature "i"
        buffer.put((byte) 0x00); // NUL byte
        buffer.putInt(Integer.MAX_VALUE); // INT32 value

        buffer.flip();

        DictEntryDecoder<DBusByte, DBusVariant> decoder = new DictEntryDecoder<>(signature);
        DecoderResult<DBusDictEntry<DBusByte, DBusVariant>> result = decoder.decode(buffer, 0);

        assertEquals(buffer.limit(), result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
        assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);

        DBusDictEntry<DBusByte, DBusVariant> entry = result.getValue();
        assertEquals(Byte.MAX_VALUE, entry.getKey().getDelegate());
        DBusInt32 int32 = (DBusInt32) entry.getValue().getDelegate();
        assertEquals(Integer.MAX_VALUE, int32.getDelegate());
    }

    @ParameterizedTest
    @MethodSource("createInvalidKeySignatures")
    void failDueToInvalidKey(ByteOrder byteOrder, String rawSignature) throws SignatureException {
        DBusSignature signature = DBusSignature.valueOf(rawSignature);
        ByteBuffer buffer = ByteBuffer.allocate(0).order(byteOrder);

        assertThrows(
                DecoderException.class,
                () -> {
                    DictEntryDecoder<DBusByte, DBusVariant> decoder =
                            new DictEntryDecoder<>(signature);
                    decoder.decode(buffer, 0);
                });
    }
}
