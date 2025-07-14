/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshals an object path from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusObjectPath
 */
public final class ObjectPathDecoder implements Decoder<ByteBuffer, DBusObjectPath> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static void logResult(DBusObjectPath value, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "OBJECT_PATH: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusObjectPath> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.skipPadding(buffer, offset, Type.OBJECT_PATH);
      consumedBytes += padding;

      int stringOffset = offset + consumedBytes;
      Decoder<ByteBuffer, DBusString> stringDecoder = new StringDecoder();
      DecoderResult<DBusString> stringResult = stringDecoder.decode(buffer, stringOffset);
      consumedBytes += stringResult.getConsumedBytes();

      DBusObjectPath path = DBusObjectPath.valueOf(stringResult.getValue().getDelegate());
      DecoderResult<DBusObjectPath> result = new DecoderResultImpl<>(consumedBytes, path);
      logResult(path, offset, padding, consumedBytes);

      return result;
    } catch (Exception e) {
      throw new DecoderException("Could not decode OBJECT_PATH.", e);
    }
  }
}
