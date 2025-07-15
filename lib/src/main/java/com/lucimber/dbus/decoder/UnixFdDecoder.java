/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusUnixFD;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a file descriptor from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusUnixFD
 */
public final class UnixFdDecoder implements Decoder<ByteBuffer, DBusUnixFD> {

  private static final Logger LOGGER = LoggerFactory.getLogger(UnixFdDecoder.class);
  private static final int TYPE_BYTES = 4;

  @Override
  public DecoderResult<DBusUnixFD> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");

    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.UNIX_FD);
      consumedBytes += padding;

      int rawValue = buffer.getInt();
      consumedBytes += TYPE_BYTES;

      DBusUnixFD value = DBusUnixFD.valueOf(rawValue);
      DecoderResult<DBusUnixFD> result = new DecoderResultImpl<>(consumedBytes, value);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "UNIX_FD: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              value, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode UNIX_FD.", t);
    }
  }
}
