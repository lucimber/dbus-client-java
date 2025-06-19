/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StringDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
  private static final String VALID_STRING = "test!ü";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeString(ByteOrder byteOrder) {
    byte[] stringBytes = VALID_STRING.getBytes(StandardCharsets.UTF_8);
    int length = stringBytes.length;
    ByteBuffer buffer = ByteBuffer.allocate(4 + length + 1).order(byteOrder);
    buffer.putInt(length);
    buffer.put(stringBytes);
    buffer.put((byte) 0x00);
    buffer.flip();

    StringDecoder decoder = new StringDecoder();
    DecoderResult<DBusString> result = decoder.decode(buffer, 0);

    assertEquals(buffer.limit(), result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    assertEquals(VALID_STRING, result.getValue().getDelegate());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void failDueToIndexLimitation(ByteOrder byteOrder) {
    byte[] stringBytes = VALID_STRING.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(4 + stringBytes.length + 1).order(byteOrder);
    // Write unsigned 2147483648 as signed int = -2147483648
    buffer.putInt(Integer.MIN_VALUE);
    buffer.put(stringBytes);
    buffer.put((byte) 0x00);
    buffer.flip();

    StringDecoder decoder = new StringDecoder();
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
