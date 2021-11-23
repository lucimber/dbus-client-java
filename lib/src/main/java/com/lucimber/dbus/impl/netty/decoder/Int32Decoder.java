package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.Int32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A decoder which unmarshalls an integer from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see Int32
 */
public final class Int32Decoder implements Decoder<ByteBuf, Int32> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

    private static final int TYPE_BYTES = 4;
    private final ByteOrder order;

    /**
     * Creates a new instance with mandatory parameters.
     *
     * @param order The order of the bytes in the buffer.
     */
    public Int32Decoder(final ByteOrder order) {
        this.order = Objects.requireNonNull(order);
    }

    private static void logResult(final Int32 value, final int offset, final int padding,
                                  final int consumedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "INT32: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
            return String.format(s, value, offset, padding, consumedBytes);
        });
    }

    @Override
    public DecoderResult<Int32> decode(final ByteBuf buffer, final int offset) throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");
        try {
            final int padding = DecoderUtils.skipPadding(buffer, offset, Type.INT32);
            final int consumedBytes = TYPE_BYTES + padding;
            final int rawValue = order.equals(ByteOrder.BIG_ENDIAN) ? buffer.readInt() : buffer.readIntLE();
            final Int32 value = Int32.valueOf(rawValue);
            final DecoderResult<Int32> result = new DecoderResultImpl<>(consumedBytes, value);
            logResult(value, offset, padding, result.getConsumedBytes());
            return result;
        } catch (Throwable t) {
            throw new DecoderException("Could not decode INT32.", t);
        }
    }
}
