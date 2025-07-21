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
import com.lucimber.dbus.util.ByteBufferPoolManager;
import com.lucimber.dbus.util.FrameRecoveryManager;
import com.lucimber.dbus.util.FrameRecoveryManager.CorruptionType;
import com.lucimber.dbus.util.FrameRecoveryManager.FrameAnalysis;
import com.lucimber.dbus.util.FrameRecoveryManager.FrameDiagnostic;
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
  private boolean frameRecoveryEnabled = true;
  private int consecutiveCorruptions = 0;
  private static final int MAX_CONSECUTIVE_CORRUPTIONS = 10;

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.error(LoggerUtils.HANDLER_LIFECYCLE, "Encountered an exception while decoding.", cause);
    ctx.fireExceptionCaught(cause);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
    while (true) {
      try {
        switch (decoderState) {
          case HEADER_PREAMBLE:
            if (in.readableBytes() < HEADER_PREAMBLE_SIZE) {
              return; // Not enough data yet
            }
            decodeHeaderPreamble(in);
            consecutiveCorruptions = 0; // Reset on successful decode
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
      } catch (CorruptedFrameException | TooLongFrameException e) {
        handleFrameCorruption(ctx, in, e);
        return; // Exit decode loop after corruption handling
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

    // Validate fieldsLen to prevent integer overflow
    if (fieldsLen < 0) {
      throw new CorruptedFrameException("Invalid header fields length: " + fieldsLen);
    }
    
    // Check for potential integer overflow in offset + 4 + fieldsLen
    if (fieldsLen > Integer.MAX_VALUE - offset - 4) {
      throw new TooLongFrameException("Header fields length too large: " + fieldsLen);
    }

    int padding = (offset + 4 + fieldsLen) % HEADER_ALIGNMENT;
    
    // Check for potential integer overflow in fieldsLen + padding
    if (fieldsLen > Integer.MAX_VALUE - padding) {
      throw new TooLongFrameException("Header fields length with padding too large: " + fieldsLen + " + " + padding);
    }
    
    if (InboundUtils.isMessageTooLong(fieldsLen + padding, frame.getBodyLength().intValue())) {
      throw new TooLongFrameException();
    }

    // Check for potential integer overflow in 4 + fieldsLen
    if (fieldsLen > Integer.MAX_VALUE - 4) {
      throw new TooLongFrameException("Header fields length too large for buffer check: " + fieldsLen);
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
    
    // Validate body length to prevent buffer overflow
    if (bodyLength < 0) {
      throw new CorruptedFrameException("Invalid body length: " + bodyLength);
    }
    
    // Check if body length exceeds reasonable limits
    if (bodyLength > 128 * 1024 * 1024) { // 128MB limit
      throw new TooLongFrameException("Body length too large: " + bodyLength);
    }
    
    // Handle empty body case
    if (bodyLength == 0) {
      frame.setBody(ByteBuffer.allocate(0).order(frame.getByteOrder()));
      return;
    }
    
    // Use buffer pool for allocation
    ByteBufferPoolManager poolManager = ByteBufferPoolManager.getInstance();
    ByteBuffer body = poolManager.acquire(bodyLength, frame.getByteOrder());
    
    // Ensure buffer has exact capacity needed
    if (body.capacity() > bodyLength) {
      body.limit(bodyLength);
    }
    
    body.put(in.nioBuffer(in.readerIndex(), bodyLength));
    body.flip(); // Reset position to 0 and set limit for reading
    frame.setBody(body);
    in.skipBytes(bodyLength); // Advance input buffer past the body
  }

  /**
   * Handles frame corruption by attempting recovery using FrameRecoveryManager.
   * 
   * @param ctx channel handler context
   * @param in incoming buffer
   * @param cause the corruption exception
   */
  private void handleFrameCorruption(ChannelHandlerContext ctx, ByteBuf in, Exception cause) {
    consecutiveCorruptions++;
    
    LOGGER.warn("Frame corruption detected (consecutive: {}): {}", consecutiveCorruptions, cause.getMessage());
    
    // Record corruption statistics
    CorruptionType corruptionType = classifyCorruption(cause);
    
    if (!frameRecoveryEnabled || consecutiveCorruptions > MAX_CONSECUTIVE_CORRUPTIONS) {
      LOGGER.error("Frame recovery disabled or too many consecutive corruptions, failing channel");
      FrameRecoveryManager.recordCorruption(corruptionType, false);
      ctx.fireExceptionCaught(cause);
      return;
    }
    
    try {
      boolean recovered = attemptFrameRecovery(in);
      FrameRecoveryManager.recordCorruption(corruptionType, recovered);
      
      if (recovered) {
        LOGGER.info("Frame recovery successful, continuing decode");
        // Reset frame state for next attempt
        resetDecoderState();
      } else {
        LOGGER.error("Frame recovery failed, failing channel");
        ctx.fireExceptionCaught(cause);
      }
    } catch (Exception recoveryException) {
      LOGGER.error("Exception during frame recovery", recoveryException);
      FrameRecoveryManager.recordCorruption(corruptionType, false);
      ctx.fireExceptionCaught(cause);
    }
  }

  /**
   * Attempts to recover from frame corruption by finding the next frame boundary.
   * 
   * @param in incoming buffer
   * @return true if recovery was successful
   */
  private boolean attemptFrameRecovery(ByteBuf in) {
    if (in.readableBytes() < 16) { // Need minimum frame size for recovery
      return false;
    }
    
    // Convert ByteBuf to ByteBuffer for FrameRecoveryManager
    ByteBuffer nioBuffer = in.nioBuffer(in.readerIndex(), in.readableBytes());
    
    // Create diagnostic information for logging
    FrameDiagnostic diagnostic = FrameRecoveryManager.createDiagnostic(nioBuffer, 64);
    LOGGER.debug("Frame corruption diagnostic:\n{}", diagnostic);
    
    // Try to find next frame boundary
    int maxScanBytes = Math.min(in.readableBytes(), 1024); // Limit scan to prevent excessive processing
    int nextBoundary = FrameRecoveryManager.findNextFrameBoundary(nioBuffer, maxScanBytes);
    
    if (nextBoundary > 0) {
      LOGGER.info("Found potential frame boundary at offset {}, skipping {} corrupted bytes", 
                  nextBoundary, nextBoundary);
      in.skipBytes(nextBoundary);
      return true;
    } else {
      // No frame boundary found, skip a small amount to avoid infinite loop
      int skipBytes = Math.min(4, in.readableBytes());
      LOGGER.debug("No frame boundary found, skipping {} bytes", skipBytes);
      in.skipBytes(skipBytes);
      return false;
    }
  }

  /**
   * Classifies the type of frame corruption based on the exception.
   * 
   * @param cause the exception that caused the corruption
   * @return the corruption type
   */
  private CorruptionType classifyCorruption(Exception cause) {
    if (cause instanceof TooLongFrameException) {
      return CorruptionType.FRAME_TOO_LARGE;
    } else if (cause instanceof CorruptedFrameException) {
      String message = cause.getMessage().toLowerCase();
      if (message.contains("invalid") && message.contains("length")) {
        return CorruptionType.INVALID_LENGTH;
      } else if (message.contains("byte order") || message.contains("endian")) {
        return CorruptionType.INVALID_ENDIAN_FLAG;
      } else if (message.contains("protocol")) {
        return CorruptionType.INVALID_PROTOCOL_VERSION;
      } else {
        return CorruptionType.PARSE_EXCEPTION;
      }
    } else {
      return CorruptionType.PARSE_EXCEPTION;
    }
  }

  /**
   * Resets the decoder state for attempting to decode the next frame.
   */
  private void resetDecoderState() {
    frame = new Frame();
    offset = 0;
    decoderState = DecoderState.HEADER_PREAMBLE;
  }

  private enum DecoderState {
    HEADER_PREAMBLE,
    HEADER_FIELDS,
    HEADER_PADDING,
    BODY
  }
}
