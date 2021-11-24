package com.lucimber.dbus.impl.netty.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusDouble;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

final class DoubleDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @Test
  void decodeDoubleOnBigEndian() {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x40, 0x02, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66};
    buffer.writeBytes(bytes);
    final double expected = 2.3;
    final DoubleDecoder decoder = new DoubleDecoder(ByteOrder.BIG_ENDIAN);
    final DecoderResult<DBusDouble> result = decoder.decode(buffer, 0);
    assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertEquals(expected, result.getValue().getDelegate());
  }

  @Test
  void decodeDoubleOnLittleEndian() {
    final ByteBuf buffer = Unpooled.buffer();
    final byte[] bytes = {0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x02, 0x40};
    buffer.writeBytes(bytes);
    final double expected = 2.3;
    final DoubleDecoder decoder = new DoubleDecoder(ByteOrder.LITTLE_ENDIAN);
    final DecoderResult<DBusDouble> result = decoder.decode(buffer, 0);
    assertEquals(bytes.length, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertEquals(expected, result.getValue().getDelegate());
  }
}
