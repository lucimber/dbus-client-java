/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.decoder;

import com.lucimber.dbus.type.DBusString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
  void decodeString(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    // UINT32 bytes
    final byte[] lengthBytes;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      lengthBytes = new byte[]{0x00, 0x00, 0x00, 0x07};
    } else {
      lengthBytes = new byte[]{0x07, 0x00, 0x00, 0x00};
    }
    buffer.writeBytes(lengthBytes);
    // UTF-8 bytes (7 bytes)
    buffer.writeBytes(VALID_STRING.getBytes(StandardCharsets.UTF_8));
    // Trailing NUL byte
    buffer.writeByte(0x00);
    final int numOfBytes = buffer.readableBytes();
    final StringDecoder decoder = new StringDecoder(byteOrder);
    final DecoderResult<DBusString> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertEquals(VALID_STRING, result.getValue().getDelegate());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void failDueToIndexLimitation(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    // UINT32 bytes (Integer.MAX_VALUE + 1 = 2147483648)
    final byte[] lengthBytes;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      lengthBytes = new byte[]{(byte) 0x80, 0x00, 0x00, 0x00};
    } else {
      lengthBytes = new byte[]{0x00, 0x00, 0x00, (byte) 0x80};
    }
    buffer.writeBytes(lengthBytes);
    // UTF-8 bytes (7 bytes)
    buffer.writeBytes(VALID_STRING.getBytes(StandardCharsets.UTF_8));
    // Trailing NUL byte
    buffer.writeByte(0x00);
    final StringDecoder decoder = new StringDecoder(byteOrder);
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
