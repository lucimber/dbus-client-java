/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusInt16;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a short from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusInt16
 */
public final class Int16Decoder implements Decoder<ByteBuffer, DBusInt16> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Int16Decoder.class);
  private static final int TYPE_BYTES = 2;

  @Override
  public DecoderResult<DBusInt16> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");

    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.INT16);
      consumedBytes += padding;

      short rawValue = buffer.getShort();
      consumedBytes += TYPE_BYTES;

      DBusInt16 value = DBusInt16.valueOf(rawValue);
      DecoderResult<DBusInt16> result = new DecoderResultImpl<>(consumedBytes, value);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "INT16: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              value, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode INT16.", t);
    }
  }
}
