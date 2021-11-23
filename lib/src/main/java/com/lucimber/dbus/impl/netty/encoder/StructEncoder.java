package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

/**
 * An encoder which encodes a struct to the D-Bus marshalling format.
 *
 * @see Encoder
 * @see Struct
 */
public final class StructEncoder implements Encoder<Struct, ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

    private final ByteBufAllocator allocator;
    private final ByteOrder order;
    private final Signature signature;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param allocator a {@link ByteBufAllocator}
     * @param order     a {@link ByteOrder}
     * @param signature a {@link Signature}
     */
    public StructEncoder(final ByteBufAllocator allocator, final ByteOrder order, final Signature signature) {
        this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
    }

    private static void logResult(final Signature signature, final int offset, final int padding,
                                  final int producedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "STRUCT: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
            return String.format(s, signature, offset, padding, producedBytes);
        });
    }

    @Override
    public EncoderResult<ByteBuf> encode(final Struct struct, final int offset) throws EncoderException {
        Objects.requireNonNull(struct, "struct must not be null");
        final ByteBuf buffer = allocator.buffer();
        try {
            int producedBytes = 0;
            final int padding = EncoderUtils.applyPadding(buffer, offset, Type.STRUCT);
            producedBytes += padding;
            final List<DBusType> list = struct.getDelegate();
            for (final DBusType value : list) {
                final int interimOffset = offset + producedBytes;
                final EncoderResult<ByteBuf> tmpResult = EncoderUtils.encode(value, interimOffset, order);
                final ByteBuf tmpBuffer = tmpResult.getBuffer();
                buffer.writeBytes(tmpBuffer);
                tmpBuffer.release();
                producedBytes += tmpResult.getProducedBytes();
            }
            final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
            logResult(signature, offset, padding, result.getProducedBytes());
            return result;
        } catch (Exception ex) {
            buffer.release();
            throw new EncoderException("Could not encode STRUCT.", ex);
        }
    }
}
