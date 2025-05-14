/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.DBusBoolean;
import com.lucimber.dbus.protocol.types.Type;
import com.lucimber.dbus.protocol.types.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls a boolean from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusBoolean
 */
public final class BooleanDecoder implements Decoder<ByteBuf, DBusBoolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final ByteOrder order;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order The order of the bytes in the buffer.
   */
  public BooleanDecoder(final ByteOrder order) {
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  private static void logResult(final DBusBoolean value, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "BOOLEAN: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusBoolean> decode(final ByteBuf buffer, final int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;
      final int padding = DecoderUtils.skipPadding(buffer, offset, Type.BOOLEAN);
      consumedBytes += padding;
      final Decoder<ByteBuf, UInt32> decoder = new UInt32Decoder(order);
      final DecoderResult<UInt32> rawResult = decoder.decode(buffer, offset + padding);
      consumedBytes += rawResult.getConsumedBytes();
      final int value = rawResult.getValue().getDelegate();
      final DBusBoolean decodedValue;
      if (Integer.compareUnsigned(value, 0) == 0) {
        decodedValue = DBusBoolean.valueOf(false);
      } else if (Integer.compareUnsigned(value, 1) == 0) {
        decodedValue = DBusBoolean.valueOf(true);
      } else {
        throw new Exception("Marshalled UINT32 is not a valid BOOLEAN value.");
      }
      final DecoderResult<DBusBoolean> result =
              new DecoderResultImpl<>(consumedBytes, decodedValue);
      logResult(decodedValue, offset, padding, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode BOOLEAN.", t);
    }
  }
}
