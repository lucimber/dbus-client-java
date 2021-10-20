package io.lucimber.dbus.impl.netty.decoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.Type;
import io.lucimber.dbus.type.TypeUtils;
import io.lucimber.dbus.type.Variant;
import io.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A decoder which unmarshalls a variant from the byte stream format used by D-Bus.
 *
 * @see Decoder
 * @see Variant
 */
public final class VariantDecoder implements Decoder<ByteBuf, Variant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_UNMARSHALLING);

    private final ByteOrder order;

    /**
     * Creates a new instance with mandatory parameters.
     *
     * @param order The order of the bytes in the buffer.
     */
    public VariantDecoder(final ByteOrder order) {
        this.order = Objects.requireNonNull(order);
    }

    private static DecoderResult<Signature> decodeSignature(final ByteBuf buffer, final int offset) {
        final Decoder<ByteBuf, Signature> decoder = new SignatureDecoder();
        return decoder.decode(buffer, offset);
    }

    private static void logResult(final Variant value, final int offset, final int consumedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "VARIANT: %s; Offset: %d; Padding: %d, Consumed bytes: %d;";
            return String.format(s, value, offset, 0, consumedBytes);
        });
    }

    @Override
    public DecoderResult<Variant> decode(final ByteBuf buffer, final int offset) throws DecoderException {
        Objects.requireNonNull(buffer, "buffer must not be null");
        try {
            int consumedBytes = 0;
            final DecoderResult<Signature> signatureResult = decodeSignature(buffer, offset);
            consumedBytes += signatureResult.getConsumedBytes();
            final Signature signature = signatureResult.getValue();
            if (signature.getQuantity() == 1) {
                final int valueOffset = offset + consumedBytes;
                final DecoderResult<Variant> contentResult = decode(buffer, signature, valueOffset);
                consumedBytes += contentResult.getConsumedBytes();
                final DecoderResult<Variant> result = new DecoderResultImpl<>(consumedBytes, contentResult.getValue());
                logResult(contentResult.getValue(), offset, result.getConsumedBytes());
                return result;
            } else {
                throw new Exception("Signature must be a single complete type.");
            }
        } catch (Throwable t) {
            throw new DecoderException("Could not decode VARIANT.", t);
        }
    }

    private DecoderResult<Variant> decode(final ByteBuf buffer, final Signature signature, final int offset)
            throws Exception {
        final char c = signature.getDelegate().charAt(0);
        final Type type = TypeUtils.getTypeFromChar(c)
                .orElseThrow(() -> new Exception("can not map char to type: " + c));
        if (type == Type.DICT_ENTRY) {
            throw new Exception("Invalid symbol in variant signature.");
        }
        final DecoderResult<?> result = DecoderUtils.decode(signature, buffer, offset, order);
        final int consumedBytes = result.getConsumedBytes();
        final DBusType value = result.getValue();
        final Variant variant = Variant.valueOf(value);
        return new DecoderResultImpl<>(consumedBytes, variant);
    }
}
