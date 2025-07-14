/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeUtils;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshals an array from the byte stream format used by D-Bus.
 *
 * @param <ValueT> The data type of the array.
 * @see Decoder
 * @see DBusArray
 */
public final class ArrayDecoder<ValueT extends DBusType> implements Decoder<ByteBuffer, DBusArray<ValueT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final DBusSignature signature;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param signature the signature describing the array
   */
  public ArrayDecoder(DBusSignature signature) {
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isArray()) {
      throw new IllegalArgumentException("signature must describe an array");
    }
  }

  private static void logResult(DBusSignature signature, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "ARRAY: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, signature, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusArray<ValueT>> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int arrayPadding = DecoderUtils.skipPadding(buffer, offset, Type.ARRAY);
      consumedBytes += arrayPadding;

      int lengthOffset = offset + consumedBytes;
      DecoderResult<DBusUInt32> lengthResult = new UInt32Decoder().decode(buffer, lengthOffset);
      DBusUInt32 length = lengthResult.getValue();
      consumedBytes += lengthResult.getConsumedBytes();

      DecoderUtils.verifyArrayLength(length);
      int arrayLength = length.getDelegate();

      DBusSignature elementSig = signature.subContainer();
      DBusArray<ValueT> array = new DBusArray<>(signature);

      int elementBytes = 0;
      while (Integer.compareUnsigned(elementBytes, arrayLength) < 0) {
        int elementOffset = offset + consumedBytes + elementBytes;
        DecoderResult<ValueT> elementResult = DecoderUtils.decode(elementSig, buffer, elementOffset);
        elementBytes += elementResult.getConsumedBytes();
        array.add(elementResult.getValue());
      }
      consumedBytes += elementBytes;

      int typePadding = 0;
      if (array.isEmpty()) {
        char c = elementSig.toString().charAt(0);
        Type type = TypeUtils.getTypeFromChar(c)
                .orElseThrow(() -> new DecoderException("Cannot map char to alignment: " + c));
        int typeOffset = offset + consumedBytes;
        typePadding = DecoderUtils.skipPadding(buffer, typeOffset, type);
        consumedBytes += typePadding;
      }

      DecoderResult<DBusArray<ValueT>> result = new DecoderResultImpl<>(consumedBytes, array);
      logResult(signature, offset, arrayPadding + typePadding, consumedBytes);

      return result;
    } catch (Exception ex) {
      throw new DecoderException("Could not decode ARRAY.", ex);
    }
  }
}
