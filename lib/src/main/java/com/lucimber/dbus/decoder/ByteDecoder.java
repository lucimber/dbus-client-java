/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A decoder which unmarshals a byte from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusByte
 */
public final class ByteDecoder implements Decoder<ByteBuffer, DBusByte> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static void logResult(DBusByte value, int offset, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "BYTE: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, 0, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusByte> decode(ByteBuffer buffer, int offset) {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 1;

      byte rawValue = buffer.get();
      DBusByte value = DBusByte.valueOf(rawValue);

      DecoderResult<DBusByte> result = new DecoderResultImpl<>(consumedBytes, value);
      logResult(value, offset, result.getConsumedBytes());

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode BYTE.", t);
    }
  }
}
