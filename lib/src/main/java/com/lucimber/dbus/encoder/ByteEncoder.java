/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An encoder which encodes a byte to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusByte
 */
public final class ByteEncoder implements Encoder<DBusByte, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private static final int TYPE_SIZE = 1;

  private static void logResult(DBusByte value, int offset, int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "BYTE: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, value, offset, 0, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuffer> encode(DBusByte value, int offset) throws EncoderException {
    Objects.requireNonNull(value, "value must not be null");
    try {
      // BYTE has 1-byte alignment, so no padding is ever needed.
      ByteBuffer buffer = ByteBuffer.allocate(TYPE_SIZE);
      buffer.put(value.getDelegate());
      buffer.flip();

      EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(TYPE_SIZE, buffer);
      logResult(value, offset, result.getProducedBytes());

      return result;
    } catch (Exception ex) {
      throw new EncoderException("Could not encode BYTE.", ex);
    }
  }
}
