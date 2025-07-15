/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.decoder.ArrayDecoder;
import com.lucimber.dbus.decoder.Decoder;
import com.lucimber.dbus.decoder.DecoderException;
import com.lucimber.dbus.decoder.DecoderResult;
import com.lucimber.dbus.decoder.UInt32Decoder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusStruct;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An inbound handler that decodes buffer into frame.
 *
 * @see ByteBuf
 * @see Frame
 */
final class FrameDecoder extends ByteToMessageDecoder {

  /**
   * The length of the header must be a multiple of eight.
   */
  private static final int HEADER_ALIGNMENT = 8;
  /**
   * The first six values in the header have a fixed combined size of twelve bytes.
   */
  private static final int HEADER_PREAMBLE_SIZE = 12;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Frame frame = new Frame();
  private int offset = 0;
  private DecoderState decoderState = DecoderState.HEADER_PREAMBLE;

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.error(LoggerUtils.HANDLER_LIFECYCLE, "Encountered an exception while decoding.", cause);
    ctx.fireExceptionCaught(cause);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    while (true) {
      switch (decoderState) {
        case HEADER_PREAMBLE:
          if (in.readableBytes() < HEADER_PREAMBLE_SIZE) {
            return; // Not enough data yet
          }
          decodeHeaderPreamble(in);
          decoderState = DecoderState.HEADER_FIELDS;
          break;

        case HEADER_FIELDS:
          if (in.readableBytes() < 4) {
            return; // Not enough data for header fields length
          }
          if (!canDecodeHeaderFields(in)) {
            return; // Wait for full header fields + padding
          }
          Map<HeaderField, DBusVariant> headerFields = decodeHeaderFields(in);
          frame.setHeaderFields(headerFields);
          decoderState = DecoderState.HEADER_PADDING;
          break;

        case HEADER_PADDING:
          int remainder = offset % HEADER_ALIGNMENT;
          if (remainder > 0) {
            int padding = HEADER_ALIGNMENT - remainder;
            if (in.readableBytes() < padding) {
              return; // Not enough data for padding
            } else {
              in.skipBytes(padding);
              offset += padding;
              decoderState = DecoderState.BODY;
            }
          } else {
            decoderState = DecoderState.BODY;
          }
          break;

        case BODY:
          if (in.readableBytes() < frame.getBodyLength().longValue()) {
            return; // Not enough data for body
          }
          copyMessageBody(in);
          out.add(frame);
          LOGGER.debug(LoggerUtils.TRANSPORT, "Decoded {}", frame);
          frame = new Frame();
          offset = 0;
          decoderState = DecoderState.HEADER_PREAMBLE;
          break;
        default:
          throw new io.netty.handler.codec.DecoderException("Unknown state");
      }
    }
  }

  /**
   * Peeks at inbound buffer and determines if the full message header can be obtained or not.
   *
   * @param in inbound buffer
   * @return TRUE, if full header can be decoded. FALSE otherwise.
   */
  private boolean canDecodeHeaderFields(ByteBuf in) {
    int fieldsLen;
    try {
      ByteBuffer tmpBuffer = in.nioBuffer().order(frame.getByteOrder());
      Decoder<ByteBuffer, DBusUInt32> decoder = new UInt32Decoder();
      DecoderResult<DBusUInt32> lengthResult = decoder.decode(tmpBuffer, offset);
      fieldsLen = lengthResult.getValue().getDelegate();
    } catch (DecoderException | IndexOutOfBoundsException e) {
      return false;
    }

    int padding = (offset + 4 + fieldsLen) % HEADER_ALIGNMENT;
    if (InboundUtils.isMessageTooLong(fieldsLen + padding, frame.getBodyLength().intValue())) {
      throw new TooLongFrameException();
    }

    return in.readableBytes() >= 4 + fieldsLen;
  }

  private void decodeHeaderPreamble(ByteBuf in) {
    try {
      frame.setByteOrder(InboundUtils.decodeByteOrder(in));
      offset += 1;

      frame.setType(InboundUtils.decodeType(in));
      offset += 1;

      frame.setFlags(InboundUtils.decodeFlags(in));
      offset += 1;

      frame.setProtocolVersion(in.readUnsignedByte());
      offset += 1;

      Decoder<ByteBuffer, DBusUInt32> decoder = new UInt32Decoder();
      ByteBuffer nioBuffer = in.nioBuffer().order(frame.getByteOrder());
      DecoderResult<DBusUInt32> bodyLengthResult = decoder.decode(nioBuffer, offset);
      offset += bodyLengthResult.getConsumedBytes();
      in.skipBytes(bodyLengthResult.getConsumedBytes());
      frame.setBodyLength(bodyLengthResult.getValue());

      DecoderResult<DBusUInt32> serialResult = decoder.decode(nioBuffer, offset);
      offset += serialResult.getConsumedBytes();
      in.skipBytes(serialResult.getConsumedBytes());
      frame.setSerial(serialResult.getValue());
    } catch (DecoderException e) {
      throw new CorruptedFrameException(e);
    }
  }

  private Map<HeaderField, DBusVariant> decodeHeaderFields(ByteBuf in) {
    try {
      DBusSignature signature = DBusSignature.valueOf("a(yv)");
      ArrayDecoder<DBusStruct> decoder = new ArrayDecoder<>(signature);
      ByteBuffer nioBuffer = in.nioBuffer().order(frame.getByteOrder());
      DecoderResult<DBusArray<DBusStruct>> result = decoder.decode(nioBuffer, offset);

      offset += result.getConsumedBytes();
      in.skipBytes(result.getConsumedBytes());

      return InboundUtils.mapHeaderFields(result.getValue());
    } catch (DecoderException e) {
      throw new CorruptedFrameException(e);
    }
  }

  private void copyMessageBody(ByteBuf in) {
    int bodyLength = frame.getBodyLength().intValue();
    ByteBuffer body = ByteBuffer.allocate(bodyLength);
    body.put(in.nioBuffer(in.readerIndex(), bodyLength));
    frame.setBody(body);
  }

  private enum DecoderState {
    HEADER_PREAMBLE,
    HEADER_FIELDS,
    HEADER_PADDING,
    BODY
  }
}
