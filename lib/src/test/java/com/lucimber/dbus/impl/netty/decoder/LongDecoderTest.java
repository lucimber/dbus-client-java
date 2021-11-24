package com.lucimber.dbus.impl.netty.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int64;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class LongDecoderTest {

  private static final String ASSERT_BUFFER_EMPTY = "Bytes left in buffer";
  private static final String ASSERT_CONSUMED_BYTES = "Consumed bytes by decoder";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void decodeSignedLong(final ByteOrder byteOrder) {
    final long value = -9223372036854775808L;
    final ByteBuf buffer = Unpooled.buffer();
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      buffer.writeLong(value);
    } else {
      buffer.writeLongLE(value);
    }
    final int numOfWrittenBytes = buffer.readableBytes();
    final Int64Decoder decoder = new Int64Decoder(byteOrder);
    final DecoderResult<Int64> result = decoder.decode(buffer, 0);
    assertEquals(numOfWrittenBytes, result.getConsumedBytes(), ASSERT_CONSUMED_BYTES);
    assertEquals(0, buffer.readableBytes(), ASSERT_BUFFER_EMPTY);
    assertEquals(value, result.getValue().getDelegate());
  }
}
