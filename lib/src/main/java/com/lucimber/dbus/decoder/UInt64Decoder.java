/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusUInt64;
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
 * A decoder which unmarshals an unsigned long from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusUInt64
 */
public final class UInt64Decoder implements Decoder<ByteBuffer, DBusUInt64> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);
  private static final int TYPE_BYTES = 8;

  private static void logResult(DBusUInt64 value, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "UINT64: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusUInt64> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.UINT64);
      consumedBytes += padding;

      long rawValue = buffer.getLong();
      consumedBytes += TYPE_BYTES;

      DBusUInt64 value = DBusUInt64.valueOf(rawValue);
      DecoderResult<DBusUInt64> result = new DecoderResultImpl<>(consumedBytes, value);
      logResult(value, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode UINT64.", t);
    }
  }
}
