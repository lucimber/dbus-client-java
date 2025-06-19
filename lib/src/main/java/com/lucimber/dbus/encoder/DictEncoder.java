/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.*;
import com.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * An encoder which encodes a dictionary to the D-Bus marshalling format using ByteBuffer.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 * @see Encoder
 * @see Dict
 */
public final class DictEncoder<KeyT extends DBusBasicType, ValueT extends DBusType>
      implements Encoder<Dict<KeyT, ValueT>, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private final ByteOrder order;
  private final Signature signature;

  /**
   * Constructs a new instance.
   *
   * @param order     The byte order of the produced bytes.
   * @param signature The full signature of the dictionary.
   */
  public DictEncoder(ByteOrder order, Signature signature) {
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
  }

  private static void logResult(Signature signature, int offset, int padding, int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "DICT: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, signature, offset, padding, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuffer> encode(Dict<KeyT, ValueT> dict, int offset) throws EncoderException {
    Objects.requireNonNull(dict, "dict must not be null");
    try {
      int padding = EncoderUtils.calculateAlignmentPadding(Type.ARRAY.getAlignment(), offset);

      // Prepare encoder for dictionary entries
      DictEntryEncoder<KeyT, ValueT> entryEncoder = new DictEntryEncoder<>(order, signature.subContainer());

      // Encode all entries
      ByteBuffer[] encodedEntries = new ByteBuffer[dict.dictionaryEntrySet().size()];
      int entryIndex = 0;
      int entryOffsetBase = offset + padding + 4; // 4 bytes reserved for array length
      int typePadding = EncoderUtils.calculateAlignmentPadding(Type.DICT_ENTRY.getAlignment(), entryOffsetBase);
      int entryBytesTotal = 0;

      for (DictEntry<KeyT, ValueT> entry : dict.dictionaryEntrySet()) {
        int entryOffset = entryOffsetBase + typePadding + entryBytesTotal;
        EncoderResult<ByteBuffer> encoded = entryEncoder.encode(entry, entryOffset);
        encodedEntries[entryIndex] = encoded.getBuffer();
        entryBytesTotal += encoded.getProducedBytes();
        entryIndex++;
      }

      // Encode the size field
      int fullArrayLength = entryBytesTotal;
      Encoder<UInt32, ByteBuffer> lengthEncoder = new UInt32Encoder(order);
      EncoderResult<ByteBuffer> lengthResult = lengthEncoder
            .encode(UInt32.valueOf(fullArrayLength), offset + padding);
      ByteBuffer lengthBuffer = lengthResult.getBuffer();

      // Compose final buffer
      int totalSize = padding + lengthResult.getProducedBytes() + typePadding + entryBytesTotal;
      ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(order);

      // Write padding
      for (int i = 0; i < padding; i++) {
        buffer.put((byte) 0);
      }

      // Write size field
      buffer.put(lengthBuffer);

      // Write type padding
      for (int i = 0; i < typePadding; i++) {
        buffer.put((byte) 0);
      }

      // Write all entries
      for (ByteBuffer entryBuf : encodedEntries) {
        buffer.put(entryBuf);
      }

      buffer.flip();

      logResult(signature, offset, padding + typePadding, totalSize);
      return new EncoderResultImpl<>(totalSize, buffer);
    } catch (Exception ex) {
      throw new EncoderException("Could not encode DICT.", ex);
    }
  }
}
