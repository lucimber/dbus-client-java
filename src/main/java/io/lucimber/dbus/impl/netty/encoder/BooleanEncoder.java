package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusBoolean;
import io.lucimber.dbus.type.Type;
import io.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * An encoder which encodes a boolean to the D-Bus marshalling format.
 *
 * @see Encoder
 * @see DBusBoolean
 */
public final class BooleanEncoder implements Encoder<DBusBoolean, ByteBuf> {

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
    public BooleanEncoder(final ByteBufAllocator allocator, final ByteOrder order) {
        this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
    }

    private static void logResult(final DBusBoolean value, final int offset, final int padding,
                                  final int producedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "BOOLEAN: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
            return String.format(s, value, offset, padding, producedBytes);
        });
    }

    @Override
    public EncoderResult<ByteBuf> encode(final DBusBoolean value, final int offset) throws EncoderException {
        Objects.requireNonNull(value, "value must not be null");
        final ByteBuf buffer = allocator.buffer();
        try {
            int producedBytes = 0;
            final int padding = EncoderUtils.applyPadding(buffer, offset, Type.BOOLEAN);
            producedBytes += padding;
            switch (order) {
                default:
                    throw new Exception("Unknown byte order");
                case BIG_ENDIAN:
                    buffer.writeInt(value.getDelegate() ? 1 : 0);
                    break;
                case LITTLE_ENDIAN:
                    buffer.writeIntLE(value.getDelegate() ? 1 : 0);
            }
            producedBytes += TYPE_SIZE;
            final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
            logResult(value, offset, padding, producedBytes);
            return result;
        } catch (Exception ex) {
            buffer.release();
            throw new EncoderException("Could not encode BOOLEAN.", ex);
        }
    }
}
