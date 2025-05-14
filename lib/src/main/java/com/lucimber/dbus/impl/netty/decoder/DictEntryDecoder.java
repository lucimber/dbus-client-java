/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.DBusBasicType;
import com.lucimber.dbus.protocol.types.DBusType;
import com.lucimber.dbus.protocol.types.DictEntry;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.protocol.types.Type;
import com.lucimber.dbus.protocol.types.TypeCode;
import com.lucimber.dbus.protocol.types.TypeUtils;
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
 * A decoder which unmarshalls a key-value pair from the byte stream format used by D-Bus.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 */
public final class DictEntryDecoder<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Decoder<ByteBuf, DictEntry<KeyT, ValueT>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private final ByteOrder order;
  private final Signature signature;

  /**
   * Creates a new instance with mandatory parameters.
   *
   * @param order     The order of the bytes in the buffer.
   * @param signature The signature of the bytes in the buffer.
   */
  public DictEntryDecoder(final ByteOrder order, final Signature signature) {
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    final List<Signature> children = signature.getChildren();
    if (children.size() != 2) {
      throw new DecoderException("Signature must consist of two single complete types.");
    }
    if (children.get(0).isContainerType()) {
      throw new DecoderException("Struct key must be a basic D-Bus type.");
    }
    if (children.get(1).isDictionaryEntry()) {
      throw new DecoderException("Dict-entry not allowed as value of dict-entry.");
    }
  }

  private static void logResult(final Signature signature, final int offset, final int padding,
                                final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "DICT_ENTRY: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, signature, offset, padding, consumedBytes);
    });
  }

  @Override
  public DecoderResult<DictEntry<KeyT, ValueT>> decode(final ByteBuf buffer, final int offset)
          throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      final int padding = DecoderUtils.calculateAlignmentPadding(Type.DICT_ENTRY, offset);
      if (padding > 0) {
        buffer.skipBytes(padding);
      }
      final List<Signature> children = signature.getChildren();
      final int byteCountBeforeKey = offset + padding;
      final char keyChar = children.get(0).toString().charAt(0);
      final TypeCode keyCode = TypeUtils.getCodeFromChar(keyChar)
              .orElseThrow(() -> new Exception("can not map char to type code: " + keyChar));
      final DecoderResult<KeyT> keyResult = DecoderUtils
              .decodeBasicType(keyCode, buffer, byteCountBeforeKey, order);
      final int byteCountBeforeValue = offset + padding + keyResult.getConsumedBytes();
      final DecoderResult<ValueT> valueResult = DecoderUtils
              .decode(children.get(1), buffer, byteCountBeforeValue, order);
      final int consumedBytes = padding + keyResult.getConsumedBytes() + valueResult.getConsumedBytes();
      final DictEntry<KeyT, ValueT> entry =
              new DictEntry<>(signature, keyResult.getValue(), valueResult.getValue());
      final DecoderResult<DictEntry<KeyT, ValueT>> result = new DecoderResultImpl<>(consumedBytes, entry);
      logResult(signature, offset, padding, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode DICT_ENTRY.", t);
    }
  }
}
