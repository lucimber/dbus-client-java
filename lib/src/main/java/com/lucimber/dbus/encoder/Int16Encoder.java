/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.Int16;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * An encoder which encodes a short to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see Int16
 */
public final class Int16Encoder implements Encoder<Int16, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private static final int TYPE_SIZE = 2;
  private final ByteOrder order;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param order The byte order for encoding.
   */
  public Int16Encoder(ByteOrder order) {
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  private static void logResult(Int16 value, int offset, int padding, int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "INT16: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, value, offset, padding, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuffer> encode(Int16 value, int offset) throws EncoderException {
    Objects.requireNonNull(value, "value must not be null");
    try {
      int padding = EncoderUtils.calculateAlignmentPadding(Type.INT16.getAlignment(), offset);
      int totalSize = padding + TYPE_SIZE;

      ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(order);
      for (int i = 0; i < padding; i++) {
        buffer.put((byte) 0);
      }
      buffer.putShort(value.getDelegate());
      buffer.flip();

      EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(totalSize, buffer);
      logResult(value, offset, padding, totalSize);

      return result;
    } catch (Exception ex) {
      throw new EncoderException("Could not encode INT16.", ex);
    }
  }
}
