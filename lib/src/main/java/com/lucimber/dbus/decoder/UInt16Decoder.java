/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.DBusUInt16;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshals an unsigned short from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusUInt16
 */
public final class UInt16Decoder implements Decoder<ByteBuffer, DBusUInt16> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);
  private static final int TYPE_BYTES = 2;

  private static void logResult(DBusUInt16 value, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "UINT16: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusUInt16> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.UINT16);
      consumedBytes += padding;

      short rawValue = buffer.getShort();
      consumedBytes += TYPE_BYTES;

      DBusUInt16 value = DBusUInt16.valueOf(rawValue);
      DecoderResult<DBusUInt16> result = new DecoderResultImpl<>(consumedBytes, value);
      logResult(value, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode UINT16.", t);
    }
  }
}
