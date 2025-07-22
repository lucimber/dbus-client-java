/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusUInt64;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals an unsigned long from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusUInt64
 */
public final class UInt64Decoder implements Decoder<ByteBuffer, DBusUInt64> {

  private static final Logger LOGGER = LoggerFactory.getLogger(UInt64Decoder.class);
  private static final int TYPE_BYTES = 8;

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

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "UINT64: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              value, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode UINT64.", t);
    }
  }
}
