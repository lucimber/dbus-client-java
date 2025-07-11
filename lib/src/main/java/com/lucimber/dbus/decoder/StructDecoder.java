/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusStruct;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshals a struct from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusStruct
 */
public final class StructDecoder implements Decoder<ByteBuffer, DBusStruct> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final DBusSignature signature;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param signature a {@link DBusSignature}; must describe a struct
   */
  public StructDecoder(DBusSignature signature) {
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isStruct()) {
      throw new IllegalArgumentException("signature must describe a struct");
    }
  }

  private static void logResult(DBusSignature signature, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "STRUCT: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, signature, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusStruct> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      // Skip alignment padding
      int padding = DecoderUtils.skipPadding(buffer, offset, Type.STRUCT);
      int consumedBytes = padding;

      // Decode fields
      List<DBusType> values = new ArrayList<>();
      DBusSignature subSignature = signature.subContainer();
      List<DBusSignature> components = (subSignature.getQuantity() == 1)
              ? List.of(subSignature)
              : subSignature.getChildren();

      for (DBusSignature sig : components) {
        int fieldOffset = offset + consumedBytes;
        DecoderResult<? extends DBusType> result = DecoderUtils.decode(sig, buffer, fieldOffset);
        values.add(result.getValue());
        consumedBytes += result.getConsumedBytes();
      }

      DBusStruct struct = new DBusStruct(signature, values);
      DecoderResult<DBusStruct> result = new DecoderResultImpl<>(consumedBytes, struct);
      logResult(signature, offset, padding, result.getConsumedBytes());

      return result;
    } catch (Exception e) {
      throw new DecoderException("Could not decode STRUCT.", e);
    }
  }
}
