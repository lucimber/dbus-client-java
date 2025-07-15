/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a byte from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusByte
 */
public final class ByteDecoder implements Decoder<ByteBuffer, DBusByte> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ByteDecoder.class);

  @Override
  public DecoderResult<DBusByte> decode(ByteBuffer buffer, int offset) {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 1;

      byte rawValue = buffer.get();
      DBusByte value = DBusByte.valueOf(rawValue);

      DecoderResult<DBusByte> result = new DecoderResultImpl<>(consumedBytes, value);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "BYTE: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              value, offset, 0, result.getConsumedBytes());

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode BYTE.", t);
    }
  }
}
