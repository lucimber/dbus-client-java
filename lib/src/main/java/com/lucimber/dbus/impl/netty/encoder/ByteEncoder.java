package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.util.LoggerUtils;
import com.lucimber.dbus.type.DBusByte;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * An encoder which encodes a byte to the D-Bus marshalling format.
 *
 * @see Encoder
 * @see DBusByte
 */
public final class ByteEncoder implements Encoder<DBusByte, ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

    private static final int TYPE_SIZE = 1;
    private final ByteBufAllocator allocator;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param allocator The buffer factory.
     */
    public ByteEncoder(final ByteBufAllocator allocator) {
        this.allocator = Objects.requireNonNull(allocator);
    }

    private static void logResult(final DBusByte value, final int offset, final int producedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "BYTE: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
            return String.format(s, value, offset, 0, producedBytes);
        });
    }

    @Override
    public EncoderResult<ByteBuf> encode(final DBusByte value, final int offset) throws EncoderException {
        Objects.requireNonNull(value, "value must not be null");
        final ByteBuf buffer = allocator.buffer();
        try {
            buffer.writeByte(value.getDelegate());
            final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(TYPE_SIZE, buffer);
            logResult(value, offset, result.getProducedBytes());
            return result;
        } catch (Exception ex) {
            buffer.release();
            throw new EncoderException("Could not encode BYTE.", ex);
        }
    }
}
