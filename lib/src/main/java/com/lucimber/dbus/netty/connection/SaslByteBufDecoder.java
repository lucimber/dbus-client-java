/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.ByteProcessor;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An inbound handler that decodes bytes to strings.
 */
final class SaslByteBufDecoder extends ReplayingDecoder<Void> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static int findEndOfLine(final ByteBuf buffer) {
    LoggerUtils.trace(LOGGER, () -> "Finding end of line (CRLF) in buffer.");
    final int index = buffer.readerIndex();
    final int length = buffer.readableBytes();
    return buffer.forEachByte(index, length, ByteProcessor.FIND_CRLF);
  }

  private static ByteBuf sliceBuffer(final ByteBuf buffer, final int eolIndex) {
    LoggerUtils.trace(LOGGER, () -> "Slicing buffer.");
    final int length = eolIndex - buffer.readerIndex();
    final ByteBuf slicedBuffer = buffer.readRetainedSlice(length);
    final int eolBytes = 2;
    buffer.skipBytes(eolBytes);
    return slicedBuffer;
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
    LoggerUtils.debug(LOGGER, () -> "Decoding from byte buffer to string.");
    // Find end of line and slice buffer
    final int eolIdx = findEndOfLine(in);
    final ByteBuf slicedBuffer = sliceBuffer(in, eolIdx);
    // Decode bytes to US ASCII string
    final String s = slicedBuffer.toString(StandardCharsets.US_ASCII);
    slicedBuffer.release();
    LoggerUtils.debug(LOGGER, () -> "Decoded from byte stream to string successfully.");
    LoggerUtils.trace(LOGGER, () -> "Decoded string: " + s);
    checkpoint();
    out.add(s);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    LoggerUtils.trace(LOGGER, () -> "Received an user event.");
    if (evt == CustomChannelEvent.SASL_AUTH_COMPLETE) {
      LoggerUtils.trace(LOGGER, () -> "Detaching from the channel pipeline.");
      ctx.pipeline().remove(this);
    }
    LoggerUtils.trace(LOGGER, () -> "Handing over user event to next handler in pipeline.");
    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    LoggerUtils.error(LOGGER, () -> "A throwable has been received.", cause);
    LoggerUtils.debug(LOGGER, () -> "Ignoring and passing throwable to next inbound handler.");
    ctx.fireExceptionCaught(cause);
  }
}
