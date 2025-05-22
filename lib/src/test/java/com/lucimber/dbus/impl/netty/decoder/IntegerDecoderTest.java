/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.Int32;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class IntegerDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void decodeSignedInteger(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    final int testValue = -1024;
    final int testValueHex = 0xFFFFFC00;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeInt(testValueHex);
    } else {
      buffer.writeIntLE(testValueHex);
    }
    final int numOfWrittenBytes = buffer.readableBytes();
    final Int32Decoder decoder = new Int32Decoder(byteOrder);
    final DecoderResult<Int32> result = decoder.decode(buffer, 0);
    assertEquals(numOfWrittenBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertEquals(testValue, result.getValue().getDelegate());
  }
}
