package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.impl.netty.encoder.ArrayEncoder;
import io.lucimber.dbus.impl.netty.encoder.Encoder;
import io.lucimber.dbus.impl.netty.encoder.EncoderResult;
import io.lucimber.dbus.impl.netty.encoder.Int32Encoder;
import io.lucimber.dbus.impl.netty.encoder.UInt32Encoder;
import io.lucimber.dbus.message.HeaderField;
import io.lucimber.dbus.message.MessageType;
import io.lucimber.dbus.type.DBusArray;
import io.lucimber.dbus.type.DBusByte;
import io.lucimber.dbus.type.Int32;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.Struct;
import io.lucimber.dbus.type.UInt32;
import io.lucimber.dbus.type.Variant;
import io.lucimber.dbus.util.LoggerUtils;
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
import java.util.Map;

final class FrameEncoder extends MessageToByteEncoder<Frame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_OUTBOUND);

    private static ByteBuf encodeByteOrder(final ByteBufAllocator allocator, final ByteOrder order) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding byte order: " + order);
        final int capacity = 1;
        final ByteBuf buffer = allocator.buffer(capacity, capacity);
        final byte bigEndian = 0x42;
        final byte littleEndian = 0x6C;
        switch (order) {
            default:
                throw new EncoderException("unknown byte order");
            case BIG_ENDIAN:
                buffer.writeByte(bigEndian);
                break;
            case LITTLE_ENDIAN:
                buffer.writeByte(littleEndian);
        }
        return buffer;
    }

    private static ByteBuf encodeMessageType(final ByteBufAllocator allocator, final MessageType type) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding message type: " + type);
        final int capacity = 1;
        final ByteBuf buffer = allocator.buffer(capacity, capacity);
        switch (type) {
            default:
                throw new EncoderException("unknown message type");
            case ERROR:
            case METHOD_CALL:
            case METHOD_RETURN:
            case SIGNAL:
                buffer.writeByte(type.getDecimalCode());
        }
        return buffer;
    }

    private static void encodeProtocolVersion(final ByteBuf buffer, final int version) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding protocol version: " + version);
        buffer.writeByte(version);
    }

    private static EncoderResult<ByteBuf> encodeHeaderFields(final Map<HeaderField, Variant> fields,
                                                             final ByteBufAllocator allocator,
                                                             final ByteOrder order, final int offset) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding header fields: " + fields);
        final DBusArray<Struct> structs = new DBusArray<>(Signature.valueOf("a(yv)"));
        for (Map.Entry<HeaderField, Variant> entry : fields.entrySet()) {
            final DBusByte dbusByte = DBusByte.valueOf((byte) entry.getKey().getDecimalCode());
            final Signature signature = Signature.valueOf("(yv)");
            final Struct struct = new Struct(signature, dbusByte, entry.getValue());
            structs.add(struct);
        }
        final Signature signature = Signature.valueOf("a(yv)");
        final Encoder<DBusArray<Struct>, ByteBuf> encoder = new ArrayEncoder<>(allocator, order, signature);
        return encoder.encode(structs, offset);
    }

    private static EncoderResult<ByteBuf> encodeBodyLength(final int bodyLength, final ByteBufAllocator allocator,
                                                           final ByteOrder order, final int offset) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding length of body: " + bodyLength);
        final Encoder<Int32, ByteBuf> encoder = new Int32Encoder(allocator, order);
        return encoder.encode(Int32.valueOf(bodyLength), offset);
    }

    private static EncoderResult<ByteBuf> encodeSerial(final ByteBufAllocator allocator, final ByteOrder order,
                                                       final UInt32 serial, final int offset) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding serial number: " + serial);
        final Encoder<UInt32, ByteBuf> encoder = new UInt32Encoder(allocator, order);
        return encoder.encode(serial, offset);
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final Frame msg, final ByteBuf out) {
        LoggerUtils.debug(LOGGER, MARKER, () -> "Marshalling a frame to the byte stream format.");
        final ByteBuf msgBuffer = ctx.alloc().buffer();
        try {
            int byteCount = 0;
            // Byte order
            final ByteBuf byteOrderBuffer = encodeByteOrder(ctx.alloc(), msg.getByteOrder());
            msgBuffer.writeBytes(byteOrderBuffer);
            byteOrderBuffer.release();
            byteCount += 1;
            LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
            // Message type
            final ByteBuf typeBuffer = encodeMessageType(ctx.alloc(), msg.getType());
            msgBuffer.writeBytes(typeBuffer);
            typeBuffer.release();
            byteCount += 1;
            LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
            // Message flags
            final int flagBytes = 1;
            msgBuffer.writeZero(flagBytes);
            byteCount += flagBytes;
            LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
            // Major protocol version
            encodeProtocolVersion(msgBuffer, msg.getProtocolVersion());
            byteCount += 1;
            LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
            // Body length
            final int bodyLength = msg.getBody() == null ? 0 : msg.getBody().readableBytes();
            EncoderResult<ByteBuf> bodyLengthResult =
                    encodeBodyLength(bodyLength, ctx.alloc(), msg.getByteOrder(), byteCount);
            byteCount += bodyLengthResult.getProducedBytes();
            final ByteBuf bodyLengthBuffer = bodyLengthResult.getBuffer();
            msgBuffer.writeBytes(bodyLengthBuffer);
            bodyLengthBuffer.release();
            LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
            // Serial
            final EncoderResult<ByteBuf> serialResult =
                    encodeSerial(ctx.alloc(), msg.getByteOrder(), msg.getSerial(), byteCount);
            byteCount += serialResult.getProducedBytes();
            final ByteBuf serialBuffer = serialResult.getBuffer();
            msgBuffer.writeBytes(serialBuffer);
            serialBuffer.release();
            LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
            // Header fields
            final EncoderResult<ByteBuf> headerFieldsResult =
                    encodeHeaderFields(msg.getHeaderFields(), ctx.alloc(), msg.getByteOrder(), byteCount);
            byteCount += headerFieldsResult.getProducedBytes();
            final ByteBuf headerFieldsBuffer = headerFieldsResult.getBuffer();
            msgBuffer.writeBytes(headerFieldsBuffer);
            headerFieldsBuffer.release();
            LoggerUtils.trace(LOGGER, MARKER, () -> "Readable bytes in message buffer: " + msgBuffer.readableBytes());
            // Pad before body
            final int headerBoundary = 8;
            final int remainder = byteCount % headerBoundary;
            if (remainder != 0) {
                final int padding = headerBoundary - remainder;
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
