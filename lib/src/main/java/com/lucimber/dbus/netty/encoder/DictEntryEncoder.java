/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.encoder;

import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DictEntry;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An encoder which encodes a key-value pair to the D-Bus marshalling format.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 * @see Encoder
 * @see DictEntry
 */
public final class DictEntryEncoder<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Encoder<DictEntry<KeyT, ValueT>, ByteBuf> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private final ByteBufAllocator allocator;
  private final ByteOrder order;
  private final Signature signature;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param allocator a {@link ByteBufAllocator}
   * @param order     a {@link ByteOrder}
   * @param signature a {@link Signature}
   */
  public DictEntryEncoder(final ByteBufAllocator allocator, final ByteOrder order, final Signature signature) {
    this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
  }

  private static void logResult(final Signature signature, final int offset, final int padding,
                                final int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "DICT_ENTRY: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, signature, offset, padding, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuf> encode(final DictEntry<KeyT, ValueT> entry, final int offset)
          throws EncoderException {
    Objects.requireNonNull(entry);
    final ByteBuf buffer = allocator.buffer();
    try {
      int producedBytes = 0;
      // Initial padding
      final int padding = EncoderUtils.applyPadding(buffer, offset, Type.DICT_ENTRY);
      producedBytes += padding;
      // Key
      final KeyT key = entry.getKey();
      final int keyOffset = offset + producedBytes;
      final EncoderResult<ByteBuf> keyResult = EncoderUtils.encode(key, keyOffset, order);
      producedBytes += keyResult.getProducedBytes();
      final ByteBuf keyBuffer = keyResult.getBuffer();
      buffer.writeBytes(keyBuffer);
      keyBuffer.release();
      // Value
      final ValueT value = entry.getValue();
      final int valueOffset = offset + producedBytes;
      final EncoderResult<ByteBuf> valueResult = EncoderUtils.encode(value, valueOffset, order);
      producedBytes += valueResult.getProducedBytes();
      final ByteBuf valueBuffer = valueResult.getBuffer();
      buffer.writeBytes(valueBuffer);
      valueBuffer.release();
      final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
      logResult(signature, offset, padding, result.getProducedBytes());
      return result;
    } catch (Exception ex) {
      buffer.release();
      throw new EncoderException("Could not encode DICT_ENTRY.", ex);
    }
  }
}
