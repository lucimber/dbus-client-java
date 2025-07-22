/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusDictEntry;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
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
 * A decoder which unmarshals a key-value pair from the byte stream format used by D-Bus.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 */
public final class DictEntryDecoder<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Decoder<ByteBuffer, DBusDictEntry<KeyT, ValueT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DictEntryDecoder.class);

  private final DBusSignature signature;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param signature the signature of the dict-entry
   */
  public DictEntryDecoder(DBusSignature signature) {
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    List<DBusSignature> children = signature.getChildren();
    if (children.size() != 2) {
      throw new DecoderException("Signature must consist of two single complete types.");
    }
    if (children.get(0).isContainerType()) {
      throw new DecoderException("Dict-entry key must be a basic type.");
    }
    if (children.get(1).isDictionaryEntry()) {
      throw new DecoderException("Nested dict-entry is not allowed.");
    }
  }

  @Override
  public DecoderResult<DBusDictEntry<KeyT, ValueT>> decode(ByteBuffer buffer, int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      int consumedBytes = 0;

      int padding = DecoderUtils.calculateAlignmentPadding(Type.DICT_ENTRY, offset);
      buffer.position(buffer.position() + padding);
      consumedBytes += padding;

      List<DBusSignature> children = signature.getChildren();
      String keySigStr = children.get(0).toString();
      char keyChar = keySigStr.charAt(0);
      TypeCode keyCode = TypeUtils.getCodeFromChar(keyChar)
              .orElseThrow(() -> new DecoderException("Cannot map char to type code: " + keyChar));

      DecoderResult<KeyT> keyResult = DecoderUtils
              .decodeBasicType(keyCode, buffer, offset + consumedBytes);
      consumedBytes += keyResult.getConsumedBytes();

      DecoderResult<ValueT> valueResult = DecoderUtils
              .decode(children.get(1), buffer, offset + consumedBytes);
      consumedBytes += valueResult.getConsumedBytes();

      DBusDictEntry<KeyT, ValueT> entry = new DBusDictEntry<>(signature, keyResult.getValue(), valueResult.getValue());
      DecoderResult<DBusDictEntry<KeyT, ValueT>> result = new DecoderResultImpl<>(consumedBytes, entry);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "DICT_ENTRY: {}; Offset: {}; Padding: {}; Consumed bytes: {};",
              signature, offset, padding, consumedBytes);

      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode DICT_ENTRY.", t);
    }
  }
}
