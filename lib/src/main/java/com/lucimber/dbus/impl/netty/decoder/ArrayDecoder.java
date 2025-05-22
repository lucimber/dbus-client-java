/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.DBusArray;
import com.lucimber.dbus.protocol.types.DBusType;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.protocol.types.Type;
import com.lucimber.dbus.protocol.types.TypeUtils;
import com.lucimber.dbus.protocol.types.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls an array from the byte stream format used by D-Bus.
 *
 * @param <ValueT> The data type of the array.
 * @see Decoder
 * @see DBusArray
 */
public final class ArrayDecoder<ValueT extends DBusType> implements Decoder<ByteBuf, DBusArray<ValueT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final ByteOrder order;
  private final Signature signature;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order     a {@link ByteOrder}
   * @param signature a {@link Signature}; must describe an array
   */
  public ArrayDecoder(final ByteOrder order, final Signature signature) {
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isArray()) {
      throw new IllegalArgumentException("signature must describe an array");
    }
  }

  private static DecoderResult<UInt32> decodeLength(final ByteBuf buffer, final int offset,
                                                    final ByteOrder order) {
    final Decoder<ByteBuf, UInt32> decoder = new UInt32Decoder(order);
    return decoder.decode(buffer, offset);
  }

  private static void logResult(final Signature signature, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "ARRAY: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, signature, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DBusArray<ValueT>> decode(final ByteBuf buffer, final int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;
      // Skip alignment padding
      final int arrayPadding = DecoderUtils.skipPadding(buffer, offset, Type.ARRAY);
      consumedBytes += arrayPadding;
      // Length of array
      final int lengthOffset = offset + consumedBytes;
      final DecoderResult<UInt32> lengthResult = decodeLength(buffer, lengthOffset, order);
      consumedBytes += lengthResult.getConsumedBytes();
      final UInt32 length = lengthResult.getValue();
      DecoderUtils.verifyArrayLength(length);
      // Elements of array
      // Alignment will be skipped by the element's decoder.
      final DBusArray<ValueT> array = new DBusArray<>(signature);
      final Signature eSignature = signature.subContainer();
      int elementBytes = 0;
      while (Integer.compareUnsigned(elementBytes, length.getDelegate()) < 0) {
        final int eOffset = offset + consumedBytes + elementBytes;
        final DecoderResult<ValueT> result = DecoderUtils.decode(eSignature, buffer, eOffset, order);
        elementBytes += result.getConsumedBytes();
        array.add(result.getValue());
      }
      consumedBytes += elementBytes;
      // Forcefully skip alignment padding of type, if the array is empty.
      int typePadding = 0; // Is ok to be zero if array is not empty.
      if (array.isEmpty()) {
        final char c = eSignature.toString().charAt(0);
        final Type type = TypeUtils.getTypeFromChar(c)
                .orElseThrow(() -> new Exception("can not map char to alignment: " + c));
        final int typeOffset = offset + consumedBytes;
        typePadding = DecoderUtils.skipPadding(buffer, typeOffset, type);
        consumedBytes += typePadding;
      }
      final DecoderResult<DBusArray<ValueT>> result = new DecoderResultImpl<>(consumedBytes, array);
      logResult(signature, offset, arrayPadding + typePadding, result.getConsumedBytes());
      return result;
    } catch (Exception ex) {
      throw new DecoderException("Could not decode ARRAY.", ex);
    }
  }
}
