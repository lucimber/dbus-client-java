/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshals a signature from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusSignature
 */
public final class SignatureDecoder implements Decoder<ByteBuffer, DBusSignature> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static void logResult(DBusSignature value, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "SIGNATURE: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusSignature> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.SIGNATURE);
      consumedBytes += padding;

      // Read 1-byte length prefix
      byte lenByte = buffer.get();
      consumedBytes += 1;
      int length = Byte.toUnsignedInt(lenByte);

      // Read `length` bytes of UTF-8 data
      byte[] bytes = new byte[length];
      buffer.get(bytes);
      consumedBytes += length;

      // Read 1-byte nul terminator (must be 0)
      byte nul = buffer.get();
      consumedBytes += 1;
      if (nul != 0) {
        throw new Exception("Expected nul-terminator after signature bytes.");
      }

      String sigStr = new String(bytes, StandardCharsets.UTF_8);
      DBusSignature signature = DBusSignature.valueOf(sigStr);
      DecoderResult<DBusSignature> result = new DecoderResultImpl<>(consumedBytes, signature);
      logResult(signature, offset, padding, consumedBytes);

      return result;
    } catch (Exception e) {
      throw new DecoderException("Could not decode SIGNATURE.", e);
    }
  }
}
