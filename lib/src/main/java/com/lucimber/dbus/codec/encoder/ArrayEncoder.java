/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeUtils;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An encoder which encodes an array to the D-Bus marshalling format using ByteBuffer.
 *
 * @param <E> The element's data type.
 * @see Encoder
 * @see DBusArray
 */
public final class ArrayEncoder<E extends DBusType> implements Encoder<DBusArray<E>, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArrayEncoder.class);

  // D-Bus specification: maximum array size is 64 MiB (67,108,864 bytes)
  private static final int MAX_ARRAY_SIZE = 67108864; // 2^26

  private final ByteOrder order;
  private final DBusSignature signature;

  /**
   * Constructs a new instance.
   *
   * @param order     the byte order of the produced bytes
   * @param signature the array signature (must be an array)
   */
  public ArrayEncoder(ByteOrder order, DBusSignature signature) {
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
    if (!signature.isArray()) {
      throw new IllegalArgumentException("signature does not describe an array");
    }
  }

  @Override
  public EncoderResult<ByteBuffer> encode(DBusArray<E> array, int offset) throws EncoderException {
    Objects.requireNonNull(array, "array must not be null");

    try {
      // Alignment of the array itself
      int padding = EncoderUtils.calculateAlignmentPadding(Type.ARRAY.getAlignment(), offset);

      // Determine element type and alignment
      char typeChar = signature.subContainer().toString().charAt(0);
      Type elementType = TypeUtils.getTypeFromChar(typeChar)
              .orElseThrow(() -> new EncoderException("Cannot map char to type: " + typeChar));
      int arraySizeBytes = 4;
      int typeOffset = offset + padding + arraySizeBytes;
      int typePadding = EncoderUtils.calculateAlignmentPadding(elementType.getAlignment(), typeOffset);

      // Encode elements into temporary buffers
      int entryOffsetBase = offset + padding + arraySizeBytes + typePadding;
      List<ByteBuffer> encodedElements = new ArrayList<>();
      int elementsSize = 0;
      for (E element : array) {
        EncoderResult<ByteBuffer> result = EncoderUtils
                .encode(element, entryOffsetBase + elementsSize, order);
        elementsSize += result.getProducedBytes();
        encodedElements.add(result.getBuffer());
      }

      // Check array size limit before encoding
      if (elementsSize > MAX_ARRAY_SIZE) {
        throw new EncoderException("Array too large: "
                + elementsSize + " bytes, maximum "
                + MAX_ARRAY_SIZE + " bytes");
      }

      // Encode the length prefix
      Encoder<DBusUInt32, ByteBuffer> lengthEncoder = new UInt32Encoder(order);
      int lengthOffset = offset + padding;
      EncoderResult<ByteBuffer> lengthResult = lengthEncoder
              .encode(DBusUInt32.valueOf(elementsSize), lengthOffset);
      ByteBuffer lengthBuffer = lengthResult.getBuffer();

      // Compose the final buffer
      int totalSize = padding + lengthResult.getProducedBytes() + typePadding + elementsSize;
      ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(order);

      // Write padding
      for (int i = 0; i < padding; i++) {
        buffer.put((byte) 0);
      }

      // Write array length
      buffer.put(lengthBuffer);

      // Write type alignment padding
      for (int i = 0; i < typePadding; i++) {
        buffer.put((byte) 0);
      }

      // Write array elements
      for (ByteBuffer part : encodedElements) {
        buffer.put(part);
      }

      buffer.flip();

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "ARRAY: {}; Offset: {}; Padding: {}; Produced bytes: {};",
              signature, offset, padding + typePadding, totalSize);

      return new EncoderResultImpl<>(totalSize, buffer);
    } catch (Exception ex) {
      throw new EncoderException("Could not encode ARRAY of type " + signature, ex);
    }
  }
}
