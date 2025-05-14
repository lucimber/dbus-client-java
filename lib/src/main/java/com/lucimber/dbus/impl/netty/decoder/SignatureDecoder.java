/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.protocol.types.DBusByte;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * A decoder which unmarshalls a signature from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see Signature
 */
public final class SignatureDecoder implements Decoder<ByteBuf, Signature> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

  private static void logResult(final Signature value, final int offset, final int consumedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "SIGNATURE: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
      return String.format(s, value, offset, 0, consumedBytes);
    });
  }

  @Override
  public DecoderResult<Signature> decode(final ByteBuf buffer, final int offset) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    try {
      final Decoder<ByteBuf, DBusByte> decoder = new ByteDecoder();
      final DecoderResult<DBusByte> byteResult = decoder.decode(buffer, offset);
      final byte b = byteResult.getValue().getDelegate();
      final byte[] bytes = new byte[Byte.toUnsignedInt(b)];
      buffer.readBytes(bytes);
      final int nulByteLength = 1;
      buffer.skipBytes(nulByteLength);
      final int consumedBytes = byteResult.getConsumedBytes() + bytes.length + nulByteLength;
      final String s = new String(bytes, StandardCharsets.UTF_8);
      final Signature signature = Signature.valueOf(s);
      final DecoderResult<Signature> result = new DecoderResultImpl<>(consumedBytes, signature);
      logResult(signature, offset, result.getConsumedBytes());
      return result;
    } catch (Throwable t) {
      throw new DecoderException("Could not decode SIGNATURE.", t);
    }
  }
}
