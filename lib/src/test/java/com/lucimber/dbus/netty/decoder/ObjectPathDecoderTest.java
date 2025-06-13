/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.decoder;

import com.lucimber.dbus.type.ObjectPath;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
  void decodeObjectPath(final ByteOrder byteOrder) throws DecoderException {
    final ByteBuf buffer = Unpooled.buffer();
    // UINT32 bytes
    final byte dec11 = 0x0B;
    final byte[] strLength;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      strLength = new byte[]{0x00, 0x00, 0x00, dec11};
    } else {
      strLength = new byte[]{dec11, 0x00, 0x00, 0x00};
    }
    buffer.writeBytes(strLength);
    // UTF-8 bytes (11 bytes)
    buffer.writeBytes(VALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8));
    // Trailing NUL byte
    buffer.writeByte(0x00);
    final int numOfBytes = buffer.readableBytes();
    final ObjectPathDecoder decoder = new ObjectPathDecoder(byteOrder);
    final DecoderResult<ObjectPath> result = decoder.decode(buffer, 0);
    assertEquals(numOfBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertEquals(VALID_OBJECT_PATH, result.getValue().getWrappedValue());
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void failDueToIndexLimitation(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    // UINT32 bytes (Integer.MAX_VALUE + 1 = 2147483648)
    final byte dec128 = (byte) 0x80;
    final byte[] strLength;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      strLength = new byte[]{dec128, 0x00, 0x00, 0x00};
    } else {
      strLength = new byte[]{0x00, 0x00, 0x00, dec128};
    }
    buffer.writeBytes(strLength);
    // UTF-8 bytes (12 bytes)
    buffer.writeBytes(VALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8));
    // Trailing NUL byte
    buffer.writeByte(0x00);
    final ObjectPathDecoder decoder = new ObjectPathDecoder(byteOrder);
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void failDueToInvalidObjectPath(final ByteOrder byteOrder) {
    final ByteBuf buffer = Unpooled.buffer();
    // UINT32 bytes
    final byte dec5 = 0x05;
    final byte[] strLength;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      strLength = new byte[]{0x00, 0x00, 0x00, dec5};
    } else {
      strLength = new byte[]{dec5, 0x00, 0x00, 0x00};
    }
    buffer.writeBytes(strLength);
    // UTF-8 bytes (5 bytes)
    buffer.writeBytes(INVALID_OBJECT_PATH.getBytes(StandardCharsets.UTF_8));
    // Trailing NUL byte
    buffer.writeByte(0x00);
    final ObjectPathDecoder decoder = new ObjectPathDecoder(byteOrder);
    assertThrows(DecoderException.class, () -> decoder.decode(buffer, 0));
  }
}
