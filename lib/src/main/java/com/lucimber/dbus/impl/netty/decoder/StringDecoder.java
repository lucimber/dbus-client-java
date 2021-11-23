package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A decoder which unmarshalls a string from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see DBusString
 */
public final class StringDecoder implements Decoder<ByteBuf, DBusString> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

    private final ByteOrder order;

    /**
     * Creates a new instance with mandatory parameters.
     *
     * @param order The order of the bytes in the buffer.
     */
    public StringDecoder(final ByteOrder order) {
        this.order = Objects.requireNonNull(order);
    }

    private static void logResult(final DBusString value, final int offset, final int padding,
                                  final int consumedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "STRING: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
            return String.format(s, value, offset, padding, consumedBytes);
        });
    }

    @Override
    public DecoderResult<DBusString> decode(final ByteBuf buffer, final int offset) throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");
        try {
            int consumedBytes = 0;
            final int padding = DecoderUtils.skipPadding(buffer, offset, Type.STRING);
            consumedBytes += padding;
            final int lengthOffset = offset + padding;
            final DecoderResult<UInt32> lengthResult = decodeStringLength(buffer, lengthOffset);
            consumedBytes += lengthResult.getConsumedBytes();
            final UInt32 length = lengthResult.getValue();
            if (Integer.compareUnsigned(length.intValue(), Integer.MAX_VALUE) > 0) {
                final String msg = "D-Bus string too long for Java's indices.";
                throw new Exception(msg);
            }
            final byte[] bytes = new byte[length.intValue()];
            buffer.readBytes(bytes);
            consumedBytes += length.intValue();
            final int nulByteLength = 1;
            buffer.skipBytes(nulByteLength);
            consumedBytes += nulByteLength;
            final String rawValue = new String(bytes, StandardCharsets.UTF_8);
            final DBusString value = DBusString.valueOf(rawValue);
            final DecoderResult<DBusString> result = new DecoderResultImpl<>(consumedBytes, value);
            logResult(value, offset, padding, result.getConsumedBytes());
            return result;
        } catch (Throwable t) {
            throw new DecoderException("Could not decode STRING.", t);
        }
    }

    private DecoderResult<UInt32> decodeStringLength(final ByteBuf buffer, final int offset) {
        final Decoder<ByteBuf, UInt32> decoder = new UInt32Decoder(order);
        return decoder.decode(buffer, offset);
    }
}
