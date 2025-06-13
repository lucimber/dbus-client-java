/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.decoder;

import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls a double from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusDouble
 */
public final class DoubleDecoder implements Decoder<ByteBuf, DBusDouble> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static final int TYPE_BYTES = 8;
  private final ByteOrder order;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order The order of the bytes in the buffer.
   */
  public DoubleDecoder(final ByteOrder order) {
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  private static void logResult(final DBusDouble value, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "DOUBLE: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusDouble> decode(final ByteBuf buffer, final int offset) {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      final int padding = DecoderUtils.skipPadding(buffer, offset, Type.DOUBLE);
      final int consumedBytes = TYPE_BYTES + padding;
      final double rawValue = order.equals(ByteOrder.BIG_ENDIAN) ? buffer.readDouble() : buffer.readDoubleLE();
      final DBusDouble value = DBusDouble.valueOf(rawValue);
      final DecoderResult<DBusDouble> result = new DecoderResultImpl<>(consumedBytes, value);
      logResult(value, offset, padding, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode DOUBLE.", t);
    }
  }
}
