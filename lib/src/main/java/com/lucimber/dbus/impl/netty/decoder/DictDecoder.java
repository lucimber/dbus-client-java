/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.DBusBasicType;
import com.lucimber.dbus.protocol.types.DBusType;
import com.lucimber.dbus.protocol.types.Dict;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.protocol.types.Type;
import com.lucimber.dbus.protocol.types.TypeCode;
import com.lucimber.dbus.protocol.types.TypeUtils;
import com.lucimber.dbus.protocol.types.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls a dictionary from the byte stream format used by D-Bus.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 * @see Decoder
 * @see Dict
 */
public final class DictDecoder<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Decoder<ByteBuf, Dict<KeyT, ValueT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);
  private final TypeCode keyTypeCode;
  private final ByteOrder order;
  private final Signature signature;
  private final Signature valueSignature;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order     a {@link ByteOrder}
   * @param signature a {@link Signature}
   */
  public DictDecoder(final ByteOrder order, final Signature signature) {
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isDictionary()) {
      throw new IllegalArgumentException("signature must describe a dictionary");
    }
    final Signature keyValueSignature = removeArrayAndDictEntryTypeCodes(signature);
    final List<Signature> children = keyValueSignature.getChildren();
    final char keyChar = children.get(0).toString().charAt(0);
    keyTypeCode = TypeUtils.getCodeFromChar(keyChar)
            .orElseThrow(() -> new RuntimeException("can not map char to type code: " + keyChar));
    valueSignature = children.get(1);
  }

  private static int skipPadding(final ByteBuf buffer, final int offset, final Type type) {
    final int padding = DecoderUtils.calculateAlignmentPadding(type, offset);
    if (padding > 0) {
      buffer.skipBytes(padding);
    }
    return padding;
  }

  private static void logResult(final Signature signature, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "ARRAY: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, signature, offset, padding, consumedBytes);
    });
  }

  private Signature removeArrayAndDictEntryTypeCodes(final Signature signature) {
    return signature.subContainer().subContainer();
  }

  @Override
  public DecoderResult<Dict<KeyT, ValueT>> decode(final ByteBuf buffer, final int offset)
          throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;
      // Skip the alignment padding of the array
      final int arrayPadding = skipPadding(buffer, offset, Type.ARRAY);
      consumedBytes += arrayPadding;
      // Decode the length of the array
      final int lengthOffset = offset + arrayPadding;
      final DecoderResult<UInt32> lengthResult = decodeArrayLength(buffer, lengthOffset);
      final UInt32 length = lengthResult.getValue();
      DecoderUtils.verifyArrayLength(length);
      consumedBytes += lengthResult.getConsumedBytes();
      // Decode entries
      final int entriesOffset = offset + consumedBytes;
      final DecoderResult<Dict<KeyT, ValueT>> result = decodeEntries(buffer, entriesOffset, length);
      consumedBytes += result.getConsumedBytes();
      result.setConsumedBytes(consumedBytes);
      logResult(signature, offset, arrayPadding, result.getConsumedBytes());
      return result;
    } catch (Exception ex) {
      buffer.release();
      throw new DecoderException("Could not decode ARRAY of DICT_ENTRY.", ex);
    }
  }

  private DecoderResult<Dict<KeyT, ValueT>> decodeEntries(final ByteBuf buffer, final int offset,
                                                          final UInt32 length) {
    final Dict<KeyT, ValueT> dict = new Dict<>(signature);
    int consumedBytes = 0;
    if (Integer.compareUnsigned(length.getDelegate(), 0) == 0) {
      final int padding = skipPadding(buffer, offset, Type.DICT_ENTRY);
      consumedBytes += padding;
      return new DecoderResultImpl<>(consumedBytes, dict);
    } else {
      while (Integer.compareUnsigned(consumedBytes, length.getDelegate()) < 0) {
        final int interimOffset = offset + consumedBytes;
        final int padding = skipPadding(buffer, interimOffset, Type.DICT_ENTRY);
        consumedBytes += padding;
        // Decode key
        final int keyOffset = offset + consumedBytes;
        final DecoderResult<KeyT> keyResult = DecoderUtils
                .decodeBasicType(keyTypeCode, buffer, keyOffset, order);
        consumedBytes += keyResult.getConsumedBytes();
        // Decode value
        final int valueOffset = offset + consumedBytes;
        final DecoderResult<ValueT> valueResult = DecoderUtils
                .decode(valueSignature, buffer, valueOffset, order);
        consumedBytes += valueResult.getConsumedBytes();
        dict.put(keyResult.getValue(), valueResult.getValue());
      }
    }
    return new DecoderResultImpl<>(consumedBytes, dict);
  }

  private DecoderResult<UInt32> decodeArrayLength(final ByteBuf buffer, final int offset) {
    final Decoder<ByteBuf, UInt32> decoder = new UInt32Decoder(order);
    return decoder.decode(buffer, offset);
  }
}
