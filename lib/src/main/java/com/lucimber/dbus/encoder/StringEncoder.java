/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An encoder which encodes a string to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusString
 */
public final class StringEncoder implements Encoder<DBusString, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private static final int NUL_TERMINATOR_LENGTH = 1;
  private final ByteOrder order;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param order The byte order of the produced bytes.
   */
  public StringEncoder(ByteOrder order) {
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  private static void logResult(DBusString value, int offset, int padding, int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "STRING: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, value, offset, padding, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuffer> encode(DBusString value, int offset) throws EncoderException {
    Objects.requireNonNull(value, "value must not be null");
    try {
      // Calculate padding
      int padding = EncoderUtils.calculateAlignmentPadding(Type.STRING.getAlignment(), offset);

      // Convert string to UTF-8 bytes
      byte[] stringBytes = value.getDelegate().getBytes(StandardCharsets.UTF_8);
      DBusUInt32 length = DBusUInt32.valueOf(stringBytes.length);

      // Encode the length using UInt32Encoder
      Encoder<DBusUInt32, ByteBuffer> lengthEncoder = new UInt32Encoder(order);
      EncoderResult<ByteBuffer> lengthResult = lengthEncoder.encode(length, offset + padding);
      ByteBuffer lengthBuffer = lengthResult.getBuffer();

      // Total buffer size = padding + 4 (length) + stringBytes.length + 1 (NUL)
      int totalSize = padding + lengthResult.getProducedBytes() + stringBytes.length + NUL_TERMINATOR_LENGTH;
      ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(order);

      // Write padding
      for (int i = 0; i < padding; i++) {
        buffer.put((byte) 0);
      }

      // Write length, string, and NUL
      buffer.put(lengthBuffer);
      buffer.put(stringBytes);
      buffer.put((byte) 0); // NUL terminator
      buffer.flip();

      EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(totalSize, buffer);
      logResult(value, offset, padding, totalSize);

      return result;
    } catch (Exception ex) {
      throw new EncoderException("Could not encode STRING.", ex);
    }
  }
}
