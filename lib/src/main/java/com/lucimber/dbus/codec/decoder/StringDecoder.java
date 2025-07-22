/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a string from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusString
 */
public final class StringDecoder implements Decoder<ByteBuffer, DBusString> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StringDecoder.class);

  @Override
  public DecoderResult<DBusString> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.STRING);
      consumedBytes += padding;

      int lengthOffset = offset + consumedBytes;
      DecoderResult<DBusUInt32> lengthResult = new UInt32Decoder().decode(buffer, lengthOffset);
      consumedBytes += lengthResult.getConsumedBytes();
      int length = lengthResult.getValue().intValue();

      if (length < 0 || length > buffer.remaining() - 1) {
        throw new Exception("D-Bus string length is invalid or exceeds buffer capacity.");
      }

      byte[] bytes = new byte[length];
      buffer.get(bytes);
      consumedBytes += length;

      // Consume nul-terminator
      byte nulByte = buffer.get();
      if (nulByte != 0) {
        throw new Exception("Expected nul-terminator byte after D-Bus string.");
      }
      consumedBytes += 1;

      String rawValue = new String(bytes, StandardCharsets.UTF_8);
      DBusString value = DBusString.valueOf(rawValue);
      DecoderResult<DBusString> result = new DecoderResultImpl<>(consumedBytes, value);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "STRING: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              value, offset, padding, consumedBytes);

      return result;
    } catch (Exception e) {
      throw new DecoderException("Could not decode STRING.", e);
    }
  }
}
