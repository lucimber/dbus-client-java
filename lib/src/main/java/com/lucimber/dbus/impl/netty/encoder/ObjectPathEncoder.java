/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.ObjectPath;
import com.lucimber.dbus.protocol.types.Type;
import com.lucimber.dbus.protocol.types.UInt32;
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
 * An encoder which encodes an object path to the D-Bus marshalling format.
 *
 * @see Encoder
 * @see ObjectPath
 */
public final class ObjectPathEncoder implements Encoder<ObjectPath, ByteBuf> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private final ByteBufAllocator allocator;
  private final ByteOrder order;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param allocator The buffer factory.
   * @param order     The order of the produced bytes.
   */
  public ObjectPathEncoder(final ByteBufAllocator allocator, final ByteOrder order) {
    this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  private static void logResult(final ObjectPath value, final int offset, final int padding,
                                final int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "OBJECT_PATH: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, value, offset, padding, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuf> encode(final ObjectPath value, final int offset) throws EncoderException {
    Objects.requireNonNull(value, "value must not be null");
    final ByteBuf buffer = allocator.buffer();
    try {
      int producedBytes = 0;
      final int padding = EncoderUtils.applyPadding(buffer, offset, Type.OBJECT_PATH);
      producedBytes += padding;
      final byte[] bytes = value.getDelegate().getBytes(StandardCharsets.UTF_8);
      final UInt32 bytesLength = UInt32.valueOf(bytes.length);
      final Encoder<UInt32, ByteBuf> sizeEncoder = new UInt32Encoder(allocator, order);
      final int sizeOffset = offset + producedBytes;
      final EncoderResult<ByteBuf> sizeResult = sizeEncoder.encode(bytesLength, sizeOffset);
      producedBytes += sizeResult.getProducedBytes();
      final ByteBuf sizeBuffer = sizeResult.getBuffer();
      buffer.writeBytes(sizeBuffer);
      sizeBuffer.release();
      buffer.writeBytes(bytes);
      producedBytes += bytesLength.getDelegate();
      final int nulByteLength = 1;
      buffer.writeZero(nulByteLength);
      producedBytes += nulByteLength;
      final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
      logResult(value, offset, padding, result.getProducedBytes());
      return result;
    } catch (Exception ex) {
      buffer.release();
      throw new EncoderException("Could not encode OBJECT_PATH.", ex);
    }
  }
}
