/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An encoder which encodes an integer to the D-Bus marshalling format.
 *
 * @see Encoder
 * @see UInt32
 */
public final class UInt32Encoder implements Encoder<UInt32, ByteBuf> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

  private static final int TYPE_SIZE = 4;
  private final ByteBufAllocator allocator;
  private final ByteOrder order;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param allocator The buffer factory.
   * @param order     The order of the produced bytes.
   */
  public UInt32Encoder(final ByteBufAllocator allocator, final ByteOrder order) {
    this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
    this.order = Objects.requireNonNull(order, "order must not be null");
  }

  private static void logResult(final UInt32 value, final int offset, final int padding,
                                final int producedBytes) {
    LoggerUtils.debug(LOGGER, MARKER, () -> {
      final String s = "UINT32: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
      return String.format(s, value, offset, padding, producedBytes);
    });
  }

  @Override
  public EncoderResult<ByteBuf> encode(final UInt32 value, final int offset) throws EncoderException {
    Objects.requireNonNull(value, "value must not be null");
    final ByteBuf buffer = allocator.buffer();
    try {
      final int padding = EncoderUtils.applyPadding(buffer, offset, Type.UINT32);
      switch (order) {
        default:
          throw new Exception("unknown byte order");
        case BIG_ENDIAN:
          buffer.writeInt(value.getDelegate());
          break;
        case LITTLE_ENDIAN:
          buffer.writeIntLE(value.getDelegate());
      }
      final int producedBytes = padding + TYPE_SIZE;
      final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
      logResult(value, offset, padding, result.getProducedBytes());
      return result;
    } catch (Exception ex) {
      buffer.release();
      throw new EncoderException("Could not encode UINT32.", ex);
    }
  }
}
