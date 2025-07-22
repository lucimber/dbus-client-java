/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeCode;
import com.lucimber.dbus.type.TypeUtils;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decoder which unmarshals a dictionary from the byte stream format used by D-Bus.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 */
public final class DictDecoder<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Decoder<ByteBuffer, DBusDict<KeyT, ValueT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DictDecoder.class);

  private final TypeCode keyTypeCode;
  private final DBusSignature signature;
  private final DBusSignature valueSignature;

  /**
   * Creates a new decoder for D-Bus dictionary types.
   *
   * @param signature the dictionary signature to decode
   * @throws IllegalArgumentException if signature is not a dictionary
   */
  public DictDecoder(DBusSignature signature) {
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isDictionary()) {
      throw new IllegalArgumentException("Signature must describe a dictionary.");
    }

    DBusSignature keyValueSig = signature.subContainer().subContainer();
    List<DBusSignature> children = keyValueSig.getChildren();
    char keyChar = children.get(0).toString().charAt(0);
    this.keyTypeCode = TypeUtils.getCodeFromChar(keyChar)
            .orElseThrow(() -> new RuntimeException("Cannot map char to type code: " + keyChar));
    this.valueSignature = children.get(1);
  }

  @Override
  public DecoderResult<DBusDict<KeyT, ValueT>> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int arrayPadding = DecoderUtils.skipPadding(buffer, offset, Type.ARRAY);
      consumedBytes += arrayPadding;

      int lengthOffset = offset + consumedBytes;
      DecoderResult<DBusUInt32> lengthResult = DecoderUtils.decodeBasicType(TypeCode.UINT32, buffer, lengthOffset);
      DBusUInt32 length = lengthResult.getValue();
      DecoderUtils.verifyArrayLength(length);
      consumedBytes += lengthResult.getConsumedBytes();

      int entriesOffset = offset + consumedBytes;
      DecoderResult<DBusDict<KeyT, ValueT>> entriesResult = decodeEntries(buffer, entriesOffset, length);
      consumedBytes += entriesResult.getConsumedBytes();

      DecoderResult<DBusDict<KeyT, ValueT>> finalResult =
              new DecoderResultImpl<>(consumedBytes, entriesResult.getValue());

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "DICT: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              signature, offset, arrayPadding, consumedBytes);

      return finalResult;
    } catch (Exception ex) {
      throw new DecoderException("Could not decode DICT.", ex);
    }
  }

  private DecoderResult<DBusDict<KeyT, ValueT>> decodeEntries(ByteBuffer buffer, int offset, DBusUInt32 length)
          throws DecoderException {
    DBusDict<KeyT, ValueT> dict = new DBusDict<>(signature);
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
