/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.codec.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DBusDictEntry;
import com.lucimber.dbus.type.DBusInt64;
import com.lucimber.dbus.type.DBusSignature;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class DictEntryEncoderTest {

    static final String PRODUCED_BYTES = "Number of produced bytes";
    static final String READABLE_BYTES = "Number of readable bytes";

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeSimpleDictEntry(ByteOrder byteOrder) {
        DBusSignature signature = DBusSignature.valueOf("{xb}");
        Encoder<DBusDictEntry<DBusInt64, DBusBoolean>, ByteBuffer> encoder =
                new DictEntryEncoder<>(byteOrder, signature);
        DBusDictEntry<DBusInt64, DBusBoolean> dictEntry =
                new DBusDictEntry<>(
                        signature, DBusInt64.valueOf(Long.MIN_VALUE), DBusBoolean.valueOf(false));
        EncoderResult<ByteBuffer> result = encoder.encode(dictEntry, 0);
        int expectedBytes = 12; // long(8) + boolean(4) ... padding between is zero
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
    }

    @ParameterizedTest
    @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
    void encodeSimpleDictEntryWithOffset(ByteOrder byteOrder) {
        DBusSignature signature = DBusSignature.valueOf("{xb}");
        Encoder<DBusDictEntry<DBusInt64, DBusBoolean>, ByteBuffer> encoder =
                new DictEntryEncoder<>(byteOrder, signature);
        DBusDictEntry<DBusInt64, DBusBoolean> dictEntry =
                new DBusDictEntry<>(
                        signature, DBusInt64.valueOf(Long.MIN_VALUE), DBusBoolean.valueOf(false));
        int offset = 5;
        EncoderResult<ByteBuffer> result = encoder.encode(dictEntry, offset);
        int expectedBytes = 15; // 3 bytes + long(8) + boolean(4) ... padding between is zero
        assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
        ByteBuffer buffer = result.getBuffer();
        assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
    }
}
