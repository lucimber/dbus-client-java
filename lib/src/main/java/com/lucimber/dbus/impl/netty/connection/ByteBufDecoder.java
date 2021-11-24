package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.impl.netty.decoder.Decoder;
import com.lucimber.dbus.impl.netty.decoder.DecoderException;
import com.lucimber.dbus.impl.netty.decoder.DecoderResult;
import com.lucimber.dbus.impl.netty.decoder.UInt32Decoder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An inbound handler that decodes the byte stream into message frames.
 *
 * @see Frame
 */
final class ByteBufDecoder extends ReplayingDecoder<ByteBufDecoder.DecoderState> {

  private static final int HEADER_ALIGNMENT = 8;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_INBOUND);
  private Frame frame;
  private int bodyLength;
  private int consumedFrameBytes;

  ByteBufDecoder() {
    super(DecoderState.SIGNATURE);
    frame = new Frame();
    consumedFrameBytes = 0;
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    LoggerUtils.error(LOGGER, () -> "Encountered an exception.", cause);
    ctx.fireExceptionCaught(cause);
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
    if (state() == DecoderState.SIGNATURE) {
      decodeMessageSignature(in);
      checkpoint(DecoderState.HEADER);
    }
    if (state() == DecoderState.HEADER) {
      final Map<HeaderField, Variant> headerFields = decodeMessageHeader(in);
      frame.setHeaderFields(headerFields);
      checkpoint(DecoderState.BODY);
    }
    if (state() == DecoderState.BODY) {
      copyMessageBody(in);
      final Frame completeFrame = frame;
      frame = new Frame();
      bodyLength = 0;
      consumedFrameBytes = 0;
      checkpoint(DecoderState.SIGNATURE);
      LoggerUtils.debug(LOGGER, MARKER, () -> "Decoded " + completeFrame);
      out.add(completeFrame);
    }
  }

  private void copyMessageBody(final ByteBuf buffer) {
    final ByteBuf messageBody = buffer.copy(buffer.readerIndex(), bodyLength);
    buffer.readerIndex(buffer.readerIndex() + bodyLength);
    frame.setBody(messageBody);
  }

  private Map<HeaderField, Variant> decodeMessageHeader(final ByteBuf buffer) throws TooLongFrameException {
    // Peek at header length and validate message length
    final int headerLength;
    try {
      final Decoder<ByteBuf, UInt32> decoder = new UInt32Decoder(frame.getByteOrder());
      final ByteBuf tmpBuffer = buffer.copy(buffer.readerIndex(), 4);
      final DecoderResult<UInt32> lengthResult = decoder.decode(tmpBuffer, 12);
      headerLength = lengthResult.getValue().getDelegate();
    } catch (DecoderException e) {
      throw new CorruptedFrameException(e);
    }
    if (InboundUtils.isMessageTooLong(headerLength, bodyLength)) {
      throw new TooLongFrameException();
    }
    // Decode all header fields
    final Map<HeaderField, Variant> headerFields;
    try {
      final DecoderResult<DBusArray<Struct>> result = InboundUtils
              .decodeHeaderFields(buffer, frame.getByteOrder());
      headerFields = InboundUtils.mapHeaderFields(result.getValue());
      consumedFrameBytes += result.getConsumedBytes();
      final int remainder = consumedFrameBytes % HEADER_ALIGNMENT;
      if (remainder > 0) {
        final int padding = HEADER_ALIGNMENT - remainder;
        buffer.skipBytes(padding);
        consumedFrameBytes += padding;
      }
    } catch (DecoderException e) {
      throw new CorruptedFrameException(e);
    }
    return headerFields;
  }

  private void decodeMessageSignature(final ByteBuf buffer) throws CorruptedFrameException {
    try {
      frame.setByteOrder(InboundUtils.decodeByteOrder(buffer));
      frame.setType(InboundUtils.decodeType(buffer));
      frame.setFlags(InboundUtils.decodeFlags(buffer));
      frame.setProtocolVersion(buffer.readUnsignedByte());
      final Decoder<ByteBuf, UInt32> decoder = new UInt32Decoder(frame.getByteOrder());
      final DecoderResult<UInt32> bodyLengthResult = decoder.decode(buffer, 4);
      bodyLength = bodyLengthResult.getValue().getDelegate();
      final DecoderResult<UInt32> serialNumberResult = decoder.decode(buffer, 8);
      frame.setSerial(serialNumberResult.getValue());
      final int signatureBytes = 12;
      consumedFrameBytes += signatureBytes;
    } catch (DecoderException e) {
      throw new CorruptedFrameException(e);
    }
  }

  public enum DecoderState {
    SIGNATURE,
    HEADER,
    BODY
  }
}
