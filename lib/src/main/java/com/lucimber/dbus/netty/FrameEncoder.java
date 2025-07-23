/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.codec.encoder.ArrayEncoder;
import com.lucimber.dbus.codec.encoder.Encoder;
import com.lucimber.dbus.codec.encoder.EncoderResult;
import com.lucimber.dbus.codec.encoder.Int32Encoder;
import com.lucimber.dbus.codec.encoder.UInt32Encoder;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusInt32;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusStruct;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import com.lucimber.dbus.util.LoggerUtils;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

final class FrameEncoder extends MessageToByteEncoder<Frame> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ByteBuf encodeByteOrder(ByteBufAllocator allocator, ByteOrder order) {
        LOGGER.trace(LoggerUtils.TRANSPORT, "Encoding byte order: {}", order);
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
        LOGGER.trace(LoggerUtils.TRANSPORT, "Encoding message type: {}", type);
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
        LOGGER.trace(LoggerUtils.TRANSPORT, "Encoding protocol version: {}", version);
        buffer.writeByte(version);
    }

    private static EncoderResult<ByteBuffer> encodeHeaderFields(
            Map<HeaderField, DBusVariant> fields, ByteOrder order, int offset) {
        LOGGER.trace(LoggerUtils.TRANSPORT, "Encoding header fields: {}", fields);
        DBusArray<DBusStruct> structs = new DBusArray<>(DBusSignature.valueOf("a(yv)"));
        for (Map.Entry<HeaderField, DBusVariant> entry : fields.entrySet()) {
            DBusByte dbusByte = DBusByte.valueOf(entry.getKey().getCode());
            DBusSignature signature = DBusSignature.valueOf("(yv)");
            DBusStruct struct = new DBusStruct(signature, dbusByte, entry.getValue());
            structs.add(struct);
        }
        DBusSignature signature = DBusSignature.valueOf("a(yv)");
        Encoder<DBusArray<DBusStruct>, ByteBuffer> encoder = new ArrayEncoder<>(order, signature);
        return encoder.encode(structs, offset);
    }

    private static EncoderResult<ByteBuffer> encodeBodyLength(
            int bodyLength, ByteOrder order, int offset) {
        LOGGER.trace(LoggerUtils.TRANSPORT, "Encoding length of body: {}", bodyLength);
        Encoder<DBusInt32, ByteBuffer> encoder = new Int32Encoder(order);
        return encoder.encode(DBusInt32.valueOf(bodyLength), offset);
    }

    private static EncoderResult<ByteBuffer> encodeSerial(
            ByteOrder order, DBusUInt32 serial, int offset) {
        LOGGER.trace(LoggerUtils.TRANSPORT, "Encoding serial number: {}", serial);
        Encoder<DBusUInt32, ByteBuffer> encoder = new UInt32Encoder(order);
        return encoder.encode(serial, offset);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Frame msg, ByteBuf out) {
        LOGGER.debug(
                LoggerUtils.TRANSPORT,
                "Encoding frame: type={}, serial={}",
                msg.getType(),
                msg.getSerial());
        ByteBuf msgBuffer = ctx.alloc().buffer();
        try {
            int byteCount = 0;
            // Byte order
            ByteBuf byteOrderBuffer = encodeByteOrder(ctx.alloc(), msg.getByteOrder());
            msgBuffer.writeBytes(byteOrderBuffer);
            byteOrderBuffer.release();
            byteCount += 1;
            // Message type
            ByteBuf typeBuffer = encodeMessageType(ctx.alloc(), msg.getType());
            msgBuffer.writeBytes(typeBuffer);
            typeBuffer.release();
            byteCount += 1;
            LOGGER.trace(
                    LoggerUtils.TRANSPORT,
                    "Readable bytes in message buffer: {}",
                    msgBuffer.readableBytes());
            // Message flags
            int flagBytes = 1;
            msgBuffer.writeZero(flagBytes);
            byteCount += flagBytes;
            LOGGER.trace(
                    LoggerUtils.TRANSPORT,
                    "Readable bytes in message buffer: {}",
                    msgBuffer.readableBytes());
            // Major protocol version
            encodeProtocolVersion(msgBuffer, msg.getProtocolVersion());
            byteCount += 1;
            LOGGER.trace(
                    LoggerUtils.TRANSPORT,
                    "Readable bytes in message buffer: {}",
                    msgBuffer.readableBytes());
            // Body length
            int bodyLength = msg.getBody() == null ? 0 : msg.getBody().remaining();
            EncoderResult<ByteBuffer> bodyLengthResult =
                    encodeBodyLength(bodyLength, msg.getByteOrder(), byteCount);
            byteCount += bodyLengthResult.getProducedBytes();
            msgBuffer.writeBytes(bodyLengthResult.getBuffer());
            LOGGER.trace(
                    LoggerUtils.TRANSPORT,
                    "Readable bytes in message buffer: {}",
                    msgBuffer.readableBytes());
            // Serial
            EncoderResult<ByteBuffer> serialResult =
                    encodeSerial(msg.getByteOrder(), msg.getSerial(), byteCount);
            byteCount += serialResult.getProducedBytes();
            msgBuffer.writeBytes(serialResult.getBuffer());
            LOGGER.trace(
                    LoggerUtils.TRANSPORT,
                    "Readable bytes in message buffer: {}",
                    msgBuffer.readableBytes());
            // Header fields
            EncoderResult<ByteBuffer> headerFieldsResult =
                    encodeHeaderFields(msg.getHeaderFields(), msg.getByteOrder(), byteCount);
            byteCount += headerFieldsResult.getProducedBytes();
            msgBuffer.writeBytes(headerFieldsResult.getBuffer());
            LOGGER.trace(
                    LoggerUtils.TRANSPORT,
                    "Readable bytes in message buffer: {}",
                    msgBuffer.readableBytes());
            // Pad before body
            int headerBoundary = 8;
            int remainder = byteCount % headerBoundary;
            if (remainder != 0) {
                int padding = headerBoundary - remainder;
                msgBuffer.writeZero(padding);
                LOGGER.trace(LoggerUtils.TRANSPORT, "Padding header with bytes: {}", padding);
                LOGGER.trace(
                        LoggerUtils.TRANSPORT,
                        "Readable bytes in message buffer: {}",
                        msgBuffer.readableBytes());
            }
            // Message body
            if (msg.getBody() != null) {
                msgBuffer.writeBytes(msg.getBody());
                LOGGER.trace(
                        LoggerUtils.TRANSPORT,
                        "Readable bytes in message buffer: {}",
                        msgBuffer.readableBytes());
            }
            // Copy message
            int bytesToWrite = msgBuffer.readableBytes();
            out.writeBytes(msgBuffer);
            LOGGER.debug(
                    LoggerUtils.TRANSPORT,
                    "Encoded {} bytes for {} frame (serial={})",
                    bytesToWrite,
                    msg.getType(),
                    msg.getSerial());
        } catch (Throwable t) {
            LOGGER.warn(LoggerUtils.TRANSPORT, "Caught exception while encoding.", t);
            throw new EncoderException(t);
        } finally {
            msgBuffer.release();
        }
    }
}
