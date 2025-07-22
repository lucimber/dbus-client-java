/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusObjectPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ObjectPathDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";
  private static final String INVALID_OBJECT_PATH = "/a//c";
  private static final String VALID_OBJECT_PATH = "/abc/d1/e_f";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeObjectPath(ByteOrder byteOrder) throws DecoderException {
  byte[] stringBytes = VALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8);
  int totalLength = 4 + stringBytes.length + 1;

  ByteBuffer buffer = ByteBuffer.allocate(totalLength).order(byteOrder);
  buffer.putInt(stringBytes.length);
  buffer.put(stringBytes);
  buffer.put((byte) 0x00);
  buffer.flip();

  ObjectPathDecoder decoder = new ObjectPathDecoder();
  DecoderResult<DBusObjectPath> result = decoder.decode(buffer, 0);

  assertEquals(totalLength, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(VALID_OBJECT_PATH, result.getValue().getWrappedValue());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void failDueToIndexLimitation(ByteOrder byteOrder) {
  byte[] stringBytes = VALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8);

  ByteBuffer buffer = ByteBuffer.allocate(4 + stringBytes.length + 1).order(byteOrder);
  buffer.putInt(Integer.MIN_VALUE); // simulate unsigned 2147483648
  buffer.put(stringBytes);
  buffer.put((byte) 0x00);
  buffer.flip();

  ObjectPathDecoder decoder = new ObjectPathDecoder();
  assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void failDueToInvalidObjectPath(ByteOrder byteOrder) {
  byte[] stringBytes = INVALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8);
  int totalLength = 4 + stringBytes.length + 1;

  ByteBuffer buffer = ByteBuffer.allocate(totalLength).order(byteOrder);
  buffer.putInt(stringBytes.length);
  buffer.put(stringBytes);
  buffer.put((byte) 0x00);
  buffer.flip();

  ObjectPathDecoder decoder = new ObjectPathDecoder();
  assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
