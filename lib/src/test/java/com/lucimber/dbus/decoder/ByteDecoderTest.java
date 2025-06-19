/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusByte;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ByteDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @Test
  void decodeByte() {
    byte maxValue = (byte) 0xFF;
    ByteBuffer buffer = ByteBuffer.allocate(1);
    buffer.put(maxValue);
    buffer.flip();

    ByteDecoder decoder = new ByteDecoder();
    DecoderResult<DBusByte> result = decoder.decode(buffer, 0);

    assertEquals(1, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.remaining(), ASSERT_BUFFER_EMPTY);
    byte expected = -1;
    assertEquals(expected, result.getValue().getDelegate());
  }
}
