/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusUnixFD;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class UnixFileDescriptorDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void decodeUnixFileDescriptor(ByteOrder byteOrder) throws DecoderException {
    ByteBuffer buffer = ByteBuffer.allocate(4).order(byteOrder);
    int value = 0xFFFFFC00; // -1024 in signed
    buffer.putInt(value);
    buffer.flip();

    int expected = -1024;
    Decoder<ByteBuffer, DBusUnixFD> decoder = new UnixFdDecoder();
    DecoderResult<DBusUnixFD> result = decoder.decode(buffer, 0);

    int numOfBytes = 4;
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    DBusUnixFD descriptor = result.getValue();
    assertEquals(expected, descriptor.getDelegate());
  }
}
