/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.Dict;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Variant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DictionaryDecoderTest {

  private static final String ARRAY_OF_ENTRIES = "a{yv}";
  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeDictionary(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf(ARRAY_OF_ENTRIES);
    String rawSig = "i";
    byte[] sigBytes = rawSig.getBytes(StandardCharsets.UTF_8);

    int dictLength = 12;
    int bufferSize = 4 + 4 + 1 + 1 + sigBytes.length + 1 + 4;
    ByteBuffer buffer = ByteBuffer.allocate(bufferSize).order(byteOrder);

    // Array length
    buffer.putInt(dictLength);
    // Padding
    buffer.putInt(0);
    // Key (BYTE)
    buffer.put((byte) 0x7F);
    // Signature of variant (length + string + nul)
    buffer.put((byte) sigBytes.length);
    buffer.put(sigBytes);
    buffer.put((byte) 0x00);
    // Variant value (INT32)
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.putInt(Integer.MAX_VALUE);
    } else {
      buffer.putInt(Integer.reverseBytes(Integer.MAX_VALUE));
    }

    buffer.flip();

    DictDecoder<DBusByte, Variant> decoder = new DictDecoder<>(signature);
    DecoderResult<Dict<DBusByte, Variant>> result = decoder.decode(buffer, 0);

    assertEquals(bufferSize, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    Dict<DBusByte, Variant> dict = result.getValue();
    assertEquals(1, dict.size());
    assertTrue(dict.containsKey(DBusByte.valueOf((byte) 0x7F)));
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeEmptyDictionary(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf(ARRAY_OF_ENTRIES);
    int bufferSize = 4 + 4;
    ByteBuffer buffer = ByteBuffer.allocate(bufferSize).order(byteOrder);

    buffer.putInt(0);      // array length = 0
    buffer.putInt(0);      // padding for DICT_ENTRY alignment
    buffer.flip();

    DictDecoder<DBusByte, Variant> decoder = new DictDecoder<>(signature);
    DecoderResult<Dict<DBusByte, Variant>> result = decoder.decode(buffer, 0);

    assertEquals(bufferSize, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    Dict<DBusByte, Variant> dict = result.getValue();
    assertTrue(dict.isEmpty());
  }
}
