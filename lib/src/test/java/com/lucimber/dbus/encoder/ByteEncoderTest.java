/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusByte;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ByteEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @Test
  void encodeByteOfMinValue() {
    Encoder<DBusByte, ByteBuffer> encoder = new ByteEncoder();
    DBusByte dbusByte = DBusByte.valueOf((byte) 0);
    EncoderResult<ByteBuffer> result = encoder.encode(dbusByte, 0);
    int expectedBytes = 1;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
    assertEquals((byte) 0x00, buffer.get(0));
  }

  @Test
  void encodeByteOfMaxValue() {
    Encoder<DBusByte, ByteBuffer> encoder = new ByteEncoder();
    DBusByte dbusByte = DBusByte.valueOf((byte) 255);
    EncoderResult<ByteBuffer> result = encoder.encode(dbusByte, 0);
    int expectedBytes = 1;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
    byte byteMaxValue = (byte) 0xFF;
    assertEquals(byteMaxValue, buffer.get(0));
  }
}
