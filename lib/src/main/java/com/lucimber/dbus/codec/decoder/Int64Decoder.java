/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusInt64;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a long from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusInt64
 */
public final class Int64Decoder implements Decoder<ByteBuffer, DBusInt64> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Int64Decoder.class);
  private static final int TYPE_BYTES = 8;

  @Override
  public DecoderResult<DBusInt64> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.INT64);
      consumedBytes += padding;

      long rawValue = buffer.getLong();
      consumedBytes += TYPE_BYTES;

      DBusInt64 value = DBusInt64.valueOf(rawValue);
      DecoderResult<DBusInt64> result = new DecoderResultImpl<>(consumedBytes, value);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "INT64: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              value, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode INT64.", t);
    }
  }
}
