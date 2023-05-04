/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
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
 * A decoder which unmarshalls an object path from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see ObjectPath
 */
public final class ObjectPathDecoder implements Decoder<ByteBuf, ObjectPath> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final ByteOrder order;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order The order of the bytes in the buffer.
   */
  public ObjectPathDecoder(final ByteOrder order) {
    this.order = Objects.requireNonNull(order);
  }

  private static void logResult(final ObjectPath value, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "OBJECT_PATH: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<ObjectPath> decode(final ByteBuf buffer, final int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;
      final int padding = DecoderUtils.skipPadding(buffer, offset, Type.OBJECT_PATH);
      consumedBytes += padding;
      final Decoder<ByteBuf, DBusString> decoder = new StringDecoder(order);
      final int stringOffset = offset + padding;
      final DecoderResult<DBusString> stringResult = decoder.decode(buffer, stringOffset);
      consumedBytes += stringResult.getConsumedBytes();
      final CharSequence sequence = stringResult.getValue().getDelegate();
      final ObjectPath path = ObjectPath.valueOf(sequence);
      final DecoderResult<ObjectPath> result = new DecoderResultImpl<>(consumedBytes, path);
      logResult(path, offset, padding, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode OBJECT_PATH.", t);
    }
  }
}
