/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusStruct;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An encoder which encodes a struct to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusStruct
 */
public final class StructEncoder implements Encoder<DBusStruct, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StructEncoder.class);

  private final ByteOrder order;
  private final DBusSignature signature;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param order     the byte order of the produced bytes
   * @param signature the signature of the struct being encoded
   */
  public StructEncoder(ByteOrder order, DBusSignature signature) {
    this.order = Objects.requireNonNull(order, "order must not be null");
    this.signature = Objects.requireNonNull(signature, "signature must not be null");
  }

  @Override
  public EncoderResult<ByteBuffer> encode(DBusStruct struct, int offset) throws EncoderException {
    Objects.requireNonNull(struct, "struct must not be null");
    try {
      int producedBytes = 0;
      int padding = EncoderUtils.calculateAlignmentPadding(Type.STRUCT.getAlignment(), offset);
      producedBytes += padding;

      List<DBusType> values = struct.getDelegate();
      ByteBuffer[] parts = new ByteBuffer[values.size()];
      int[] sizes = new int[values.size()];

      // Encode each field individually
      for (int i = 0; i < values.size(); i++) {
        DBusType value = values.get(i);
        int interimOffset = offset + producedBytes;
        EncoderResult<ByteBuffer> partResult = EncoderUtils.encode(value, interimOffset, order);
        parts[i] = partResult.getBuffer();
        sizes[i] = partResult.getProducedBytes();
        producedBytes += sizes[i];
      }

      // Allocate final buffer with total size
      ByteBuffer buffer = ByteBuffer.allocate(producedBytes).order(order);
      for (int i = 0; i < padding; i++) {
        buffer.put((byte) 0);
      }
      for (ByteBuffer part : parts) {
        buffer.put(part);
      }
      buffer.flip();

      EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(producedBytes, buffer);

      LOGGER.debug(LoggerUtils.MARSHALLING,
              "STRUCT: {}; Offset: {}; Padding: {}; Produced bytes: {};",
              signature, offset, padding, producedBytes);

      return result;
    } catch (Exception ex) {
      throw new EncoderException("Could not encode STRUCT.", ex);
    }
  }
}
