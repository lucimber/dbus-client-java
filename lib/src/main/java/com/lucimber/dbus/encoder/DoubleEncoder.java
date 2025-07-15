/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An encoder which encodes a double to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusDouble
 */
public final class DoubleEncoder implements Encoder<DBusDouble, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DoubleEncoder.class);

  private static final int TYPE_SIZE = 8;
  private final ByteOrder order;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param order The byte order of the produced bytes.
   */
  public DoubleEncoder(ByteOrder order) {
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  @Override
  public EncoderResult<ByteBuffer> encode(DBusDouble value, int offset) throws EncoderException {
    Objects.requireNonNull(value, "value must not be null");
    try {
      int padding = EncoderUtils.calculateAlignmentPadding(Type.DOUBLE.getAlignment(), offset);
      int totalSize = padding + TYPE_SIZE;

      ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(order);
      for (int i = 0; i < padding; i++) {
        buffer.put((byte) 0);
      }
      buffer.putDouble(value.getDelegate());
      buffer.flip();

      EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(totalSize, buffer);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "DOUBLE: {}; Offset: {}; Padding: {}; Produced bytes: {};",
              value, offset, padding, totalSize);

      return result;
    } catch (Exception ex) {
      throw new EncoderException("Could not encode DOUBLE.", ex);
    }
  }
}
