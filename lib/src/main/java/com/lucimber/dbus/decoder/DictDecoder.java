/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.*;
import com.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * A decoder which unmarshals a dictionary from the byte stream format used by D-Bus.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 */
public final class DictDecoder<KeyT extends DBusBasicType, ValueT extends DBusType>
      implements Decoder<ByteBuffer, Dict<KeyT, ValueT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final TypeCode keyTypeCode;
  private final Signature signature;
  private final Signature valueSignature;

  public DictDecoder(Signature signature) {
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isDictionary()) {
      throw new IllegalArgumentException("Signature must describe a dictionary.");
    }

    Signature keyValueSig = signature.subContainer().subContainer();
    List<Signature> children = keyValueSig.getChildren();
    char keyChar = children.get(0).toString().charAt(0);
    this.keyTypeCode = TypeUtils.getCodeFromChar(keyChar)
          .orElseThrow(() -> new RuntimeException("Cannot map char to type code: " + keyChar));
    this.valueSignature = children.get(1);
  }

  private static void logResult(Signature signature, int offset, int padding, int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () ->{
      String s = "DICT: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, signature, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<Dict<KeyT, ValueT>> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int arrayPadding = DecoderUtils.skipPadding(buffer, offset, Type.ARRAY);
      consumedBytes += arrayPadding;

      int lengthOffset = offset + consumedBytes;
      DecoderResult<UInt32> lengthResult = DecoderUtils.decodeBasicType(TypeCode.UINT32, buffer, lengthOffset);
      UInt32 length = lengthResult.getValue();
      DecoderUtils.verifyArrayLength(length);
      consumedBytes += lengthResult.getConsumedBytes();

      int entriesOffset = offset + consumedBytes;
      DecoderResult<Dict<KeyT, ValueT>> entriesResult = decodeEntries(buffer, entriesOffset, length);
      consumedBytes += entriesResult.getConsumedBytes();

      DecoderResult<Dict<KeyT, ValueT>> finalResult = new DecoderResultImpl<>(consumedBytes, entriesResult.getValue());
      logResult(signature, offset, arrayPadding, consumedBytes);

      return finalResult;
    } catch (Exception ex) {
      throw new DecoderException("Could not decode DICT.", ex);
    }
  }

  private DecoderResult<Dict<KeyT, ValueT>> decodeEntries(ByteBuffer buffer, int offset, UInt32 length)
        throws DecoderException {
    Dict<KeyT, ValueT> dict = new Dict<>(signature);
    int consumedBytes = 0;

    if (length.getDelegate() == 0) {
      int padding = DecoderUtils.skipPadding(buffer, offset, Type.DICT_ENTRY);
      consumedBytes += padding;
      return new DecoderResultImpl<>(consumedBytes, dict);
    }

    while (Integer.compareUnsigned(consumedBytes, length.getDelegate()) < 0) {
      int entryOffset = offset + consumedBytes;
      int padding = DecoderUtils.skipPadding(buffer, entryOffset, Type.DICT_ENTRY);
      consumedBytes += padding;

      int keyOffset = offset + consumedBytes;
      DecoderResult<KeyT> keyResult = DecoderUtils.decodeBasicType(keyTypeCode, buffer, keyOffset);
      consumedBytes += keyResult.getConsumedBytes();

      int valueOffset = offset + consumedBytes;
      DecoderResult<ValueT> valueResult = DecoderUtils.decode(valueSignature, buffer, valueOffset);
      consumedBytes += valueResult.getConsumedBytes();

      dict.put(keyResult.getValue(), valueResult.getValue());
    }

    return new DecoderResultImpl<>(consumedBytes, dict);
  }
}
