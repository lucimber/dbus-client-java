/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.encoder;

import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An encoder which encodes a signature to the D-Bus marshalling format.
 *
 * @see Encoder
 * @see Signature
 */
public final class SignatureEncoder implements Encoder<Signature, ByteBuf> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private final ByteBufAllocator allocator;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param allocator The buffer factory.
   */
  public SignatureEncoder(final ByteBufAllocator allocator) {
    this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
  }

  private static void logResult(final Signature signature, final int offset, final int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "SIGNATURE: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, signature, offset, 0, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuf> encode(final Signature signature, final int offset) throws EncoderException {
    Objects.requireNonNull(signature, "signature must not be null");
    final ByteBuf buffer = allocator.buffer();
    try {
      final String value = signature.toString();
      final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      final int bytesLength = bytes.length;
      final ByteEncoder encoder = new ByteEncoder(allocator);
      final EncoderResult<ByteBuf> sizeResult = encoder.encode(DBusByte.valueOf((byte) bytesLength), offset);
      final ByteBuf sizeBuffer = sizeResult.getBuffer();
      buffer.writeBytes(sizeBuffer);
      sizeBuffer.release();
      buffer.writeBytes(bytes);
      final int nulByteLength = 1;
      buffer.writeZero(nulByteLength);
      final int producedBytes = sizeResult.getProducedBytes() + bytesLength + nulByteLength;
      final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
      logResult(signature, offset, result.getProducedBytes());
      return result;
    } catch (Exception ex) {
      buffer.release();
      throw new EncoderException("Could not encode SIGNATURE.", ex);
    }
  }
}
