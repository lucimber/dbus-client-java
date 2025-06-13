/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.netty.encoder.*;
import com.lucimber.dbus.type.*;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Map;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

final class FrameEncoder extends MessageToByteEncoder<Frame> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_OUTBOUND);

  private static ByteBuf encodeByteOrder(ByteBufAllocator allocator, ByteOrder order) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding byte order: " + order);
    final int capacity = 1;
    final ByteBuf buffer = allocator.buffer(capacity, capacity);
    final byte bigEndian = 0x42;
    final byte littleEndian = 0x6C;
    if (order.equals(BIG_ENDIAN)) {
      buffer.writeByte(bigEndian);
    } else if (order.equals(LITTLE_ENDIAN)) {
      buffer.writeByte(littleEndian);
    } else {
      throw new EncoderException("unknown byte order");
    }
    return buffer;
  }

  private static ByteBuf encodeMessageType(ByteBufAllocator allocator, MessageType type) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding message type: " + type);
    final int capacity = 1;
    final ByteBuf buffer = allocator.buffer(capacity, capacity);
    switch (type) {
      case ERROR:
      case METHOD_CALL:
      case METHOD_RETURN:
      case SIGNAL:
        buffer.writeByte(type.getCode());
        break;
      default:
        throw new EncoderException("unknown message type");
    }
    return buffer;
  }

  private static void encodeProtocolVersion(ByteBuf buffer, int version) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding protocol version: " + version);
    buffer.writeByte(version);
  }

  private static EncoderResult<ByteBuf> encodeHeaderFields(Map<HeaderField, Variant> fields,
                                                           ByteBufAllocator allocator,
                                                           ByteOrder order, int offset) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding header fields: " + fields);
    DBusArray<Struct> structs = new DBusArray<>(Signature.valueOf("a(yv)"));
    for (Map.Entry<HeaderField, Variant> entry : fields.entrySet()) {
      DBusByte dbusByte = DBusByte.valueOf(entry.getKey().getCode());
      Signature signature = Signature.valueOf("(yv)");
      Struct struct = new Struct(signature, dbusByte, entry.getValue());
      structs.add(struct);
    }
    Signature signature = Signature.valueOf("a(yv)");
    Encoder<DBusArray<Struct>, ByteBuf> encoder = new ArrayEncoder<>(allocator, order, signature);
    return encoder.encode(structs, offset);
  }

  private static EncoderResult<ByteBuf> encodeBodyLength(int bodyLength, ByteBufAllocator allocator,
                                                         ByteOrder order, int offset) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding length of body: " + bodyLength);
    Encoder<Int32, ByteBuf> encoder = new Int32Encoder(allocator, order);
    return encoder.encode(Int32.valueOf(bodyLength), offset);
  }

  private static EncoderResult<ByteBuf> encodeSerial(ByteBufAllocator allocator, ByteOrder order,
                                                     UInt32 serial, int offset) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding serial number: " + serial);
    Encoder<UInt32, ByteBuf> encoder = new UInt32Encoder(allocator, order);
    return encoder.encode(serial, offset);
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Frame msg, ByteBuf out) {
    LoggerUtils.debug(LOGGER, MARKER, () -> "Marshalling a frame to the byte stream format.");
    ByteBuf msgBuffer = ctx.alloc().buffer();
    try {
      int byteCount = 0;
      // Byte order
      ByteBuf byteOrderBuffer = encodeByteOrder(ctx.alloc(), msg.getByteOrder());
      msgBuffer.writeBytes(byteOrderBuffer);
      byteOrderBuffer.release();
      byteCount += 1;
      LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
      // Message type
      ByteBuf typeBuffer = encodeMessageType(ctx.alloc(), msg.getType());
      msgBuffer.writeBytes(typeBuffer);
      typeBuffer.release();
      byteCount += 1;
      LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
      // Message flags
      int flagBytes = 1;
      msgBuffer.writeZero(flagBytes);
      byteCount += flagBytes;
      LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
      // Major protocol version
      encodeProtocolVersion(msgBuffer, msg.getProtocolVersion());
      byteCount += 1;
      LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
      // Body length
      int bodyLength = msg.getBody() == null ? 0 : msg.getBody().readableBytes();
      EncoderResult<ByteBuf> bodyLengthResult =
            encodeBodyLength(bodyLength, ctx.alloc(), msg.getByteOrder(), byteCount);
      byteCount += bodyLengthResult.getProducedBytes();
      ByteBuf bodyLengthBuffer = bodyLengthResult.getBuffer();
      msgBuffer.writeBytes(bodyLengthBuffer);
      bodyLengthBuffer.release();
      LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
      // Serial
      EncoderResult<ByteBuf> serialResult =
            encodeSerial(ctx.alloc(), msg.getByteOrder(), msg.getSerial(), byteCount);
      byteCount += serialResult.getProducedBytes();
      ByteBuf serialBuffer = serialResult.getBuffer();
      msgBuffer.writeBytes(serialBuffer);
      serialBuffer.release();
      LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
      // Header fields
      EncoderResult<ByteBuf> headerFieldsResult =
            encodeHeaderFields(msg.getHeaderFields(), ctx.alloc(), msg.getByteOrder(), byteCount);
      byteCount += headerFieldsResult.getProducedBytes();
      ByteBuf headerFieldsBuffer = headerFieldsResult.getBuffer();
      msgBuffer.writeBytes(headerFieldsBuffer);
      headerFieldsBuffer.release();
      LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
      // Pad before body
      int headerBoundary = 8;
      int remainder = byteCount % headerBoundary;
      if (remainder != 0) {
        int padding = headerBoundary - remainder;
        msgBuffer.writeZero(padding);
        LoggerUtils.trace(LOGGER, MARKER, () -> "Padding header with bytes: " + padding);
        LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: "
              + msgBuffer.readableBytes());
      }
      // Message body
      if (msg.getBody() != null) {
        msgBuffer.writeBytes(msg.getBody());
        msg.getBody().release();
        LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: "
              + msgBuffer.readableBytes());
      }
      // Copy message
      out.writeBytes(msgBuffer);
    } catch (Throwable t) {
      LoggerUtils.warn(LOGGER, MARKER, () -> "Caught " + t);
      throw new EncoderException(t);
    } finally {
      msgBuffer.release();
    }
  }
}
