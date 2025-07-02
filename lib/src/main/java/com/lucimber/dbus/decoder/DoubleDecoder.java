/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshals a double from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusDouble
 */
public final class DoubleDecoder implements Decoder<ByteBuffer, DBusDouble> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static final int TYPE_BYTES = 8;

  private static void logResult(DBusDouble value, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "DOUBLE: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusDouble> decode(ByteBuffer buffer, int offset) {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;
      int padding = DecoderUtils.skipPadding(buffer, offset, Type.DOUBLE);
      consumedBytes += padding;

      double rawValue = buffer.getDouble();
      consumedBytes += TYPE_BYTES;

      DBusDouble value = DBusDouble.valueOf(rawValue);
      DecoderResult<DBusDouble> result = new DecoderResultImpl<>(consumedBytes, value);
      logResult(value, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode DOUBLE.", t);
    }
  }
}
