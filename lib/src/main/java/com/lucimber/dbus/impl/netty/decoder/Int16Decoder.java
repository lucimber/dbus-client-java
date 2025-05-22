/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int16;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls a short from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see Int16
 */
public final class Int16Decoder implements Decoder<ByteBuf, Int16> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static final int TYPE_BYTES = 2;
  private final ByteOrder order;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order The order of the bytes in the buffer.
   */
  public Int16Decoder(final ByteOrder order) {
    this.order = Objects.requireNonNull(order);
  }

  private static void logResult(final Int16 value, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "INT16: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<Int16> decode(final ByteBuf buffer, final int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      final int padding = DecoderUtils.skipPadding(buffer, offset, Type.INT16);
      final int consumedBytes = TYPE_BYTES + padding;
      final short rawValue = order.equals(ByteOrder.BIG_ENDIAN) ? buffer.readShort() : buffer.readShortLE();
      final Int16 value = Int16.valueOf(rawValue);
      final DecoderResult<Int16> result = new DecoderResultImpl<>(consumedBytes, value);
      logResult(value, offset, padding, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode INT16.", t);
    }
  }
}
