/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An encoder which encodes a signature to the D-Bus marshalling format using ByteBuffer.
 *
 * @see Encoder
 * @see Signature
 */
public final class SignatureEncoder implements Encoder<Signature, ByteBuffer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private static final int NUL_TERMINATOR_LENGTH = 1;

  private static void logResult(Signature signature, int offset, int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      String s = "SIGNATURE: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, signature, offset, 0, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuffer> encode(Signature signature, int offset) throws EncoderException {
    Objects.requireNonNull(signature, "signature must not be null");
    try {
      String value = signature.toString();
      byte[] signatureBytes = value.getBytes(StandardCharsets.UTF_8);
      int length = signatureBytes.length;

      // Encode the length as a single byte
      DBusByte size = DBusByte.valueOf((byte) length);
      Encoder<DBusByte, ByteBuffer> byteEncoder = new ByteEncoder();
      EncoderResult<ByteBuffer> lengthResult = byteEncoder.encode(size, offset);
      ByteBuffer lengthBuffer = lengthResult.getBuffer();

      // Allocate the complete buffer
      int totalSize = lengthResult.getProducedBytes() + length + NUL_TERMINATOR_LENGTH;
      ByteBuffer buffer = ByteBuffer.allocate(totalSize);
      buffer.put(lengthBuffer);
      buffer.put(signatureBytes);
      buffer.put((byte) 0); // NUL terminator
      buffer.flip();

      EncoderResult<ByteBuffer> result = new EncoderResultImpl<>(totalSize, buffer);
      logResult(signature, offset, totalSize);

      return result;
    } catch (Exception ex) {
      throw new EncoderException("Could not encode SIGNATURE.", ex);
    }
  }
}
