/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusContainerType;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusVariant;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An encoder which encodes a variant to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see DBusVariant
 */
public final class VariantEncoder implements Encoder<DBusVariant, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private final ByteOrder order;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param order the byte order of the produced bytes.
   */
  public VariantEncoder(ByteOrder order) {
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  private static void logResult(DBusVariant value, int offset, int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "VARIANT: %s; Offset: %d; Padding: %d, Produced bytes: %d;";
      return String.format(s, value, offset, 0, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuffer> encode(DBusVariant variant, int offset) throws EncoderException {
    Objects.requireNonNull(variant, "variant must not be null");
    try {
      int producedBytes = 0;
      ByteBuffer output;

      // Encode signature
      DBusSignature contentSignature = determineContentSignature(variant.getDelegate());
      Encoder<DBusSignature, ByteBuffer> sigEncoder = new SignatureEncoder();
      EncoderResult<ByteBuffer> sigResult = sigEncoder.encode(contentSignature, offset);
      final ByteBuffer sigBuffer = sigResult.getBuffer();
      producedBytes += sigResult.getProducedBytes();

      // Encode value
      DBusType value = variant.getDelegate();
      int valueOffset = offset + producedBytes;
      EncoderResult<ByteBuffer> valueResult = EncoderUtils.encode(value, valueOffset, order);
      final ByteBuffer valueBuffer = valueResult.getBuffer();
      producedBytes += valueResult.getProducedBytes();

      // Combine buffers
      output = ByteBuffer.allocate(producedBytes);
      output.put(sigBuffer);
      output.put(valueBuffer);
      output.flip();

      EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(producedBytes, output);
      logResult(variant, offset, producedBytes);

      return result;
    } catch (Exception ex) {
      throw new EncoderException("Could not encode VARIANT.", ex);
    }
  }

  private DBusSignature determineContentSignature(DBusType content) throws Exception {
    if (content instanceof DBusBasicType) {
      return DBusSignature.valueOf(String.valueOf(content.getType().getCode().getChar()));
    } else if (content instanceof DBusContainerType containerType) {
      return containerType.getSignature();
    } else {
      throw new Exception("Cannot determine content signature");
    }
  }
}
