/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusInt32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IntegerDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeSignedInteger(ByteOrder byteOrder) {
  int testValue = -1024;
  int testValueHex = 0xFFFFFC00;

  ByteBuffer buffer = ByteBuffer.allocate(4).order(byteOrder);
  buffer.putInt(testValueHex);
  buffer.flip();

  Int32Decoder decoder = new Int32Decoder();
  DecoderResult<DBusInt32> result = decoder.decode(buffer, 0);

  assertEquals(4, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
  assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
  assertEquals(testValue, result.getValue().getDelegate());
  }
}
