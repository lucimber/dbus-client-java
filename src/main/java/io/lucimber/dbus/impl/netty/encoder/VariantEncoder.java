package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusBasicType;
import io.lucimber.dbus.type.DBusContainerType;
import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.Variant;
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
 * An encoder which encodes a variant to the D-Bus marshalling format.
 *
 * @see Encoder
 * @see Variant
 */
public final class VariantEncoder implements Encoder<Variant, ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

    private final ByteBufAllocator allocator;
    private final ByteOrder order;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param allocator a {@link ByteBufAllocator}
     * @param order     a {@link ByteOrder}
     */
    public VariantEncoder(final ByteBufAllocator allocator, final ByteOrder order) {
        this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
    }

    private static void logResult(final Variant value, final int offset, final int producedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "VARIANT: %s; Offset: %d; Padding: %d, Produced bytes: %d;";
            return String.format(s, value, offset, 0, producedBytes);
        });
    }

    @Override
    public EncoderResult<ByteBuf> encode(final Variant variant, final int offset) throws EncoderException {
        Objects.requireNonNull(variant, "variant must not be null");
        final ByteBuf buffer = allocator.buffer();
        try {
            int producedBytes = 0;
            // Value's signature
            final Signature contentSignature = determineContentSignature(variant.getDelegate());
            final Encoder<Signature, ByteBuf> sigEncoder = new SignatureEncoder(allocator);
            final EncoderResult<ByteBuf> sigResult = sigEncoder.encode(contentSignature, offset);
            producedBytes += sigResult.getProducedBytes();
            final ByteBuf signatureBuffer = sigResult.getBuffer();
            buffer.writeBytes(signatureBuffer);
            signatureBuffer.release();
            // Value itself
            final DBusType value = variant.getDelegate();
            final int valueOffset = offset + producedBytes;
            final EncoderResult<ByteBuf> valueResult = EncoderUtils.encode(value, valueOffset, order);
            producedBytes += valueResult.getProducedBytes();
            final ByteBuf valueBuffer = valueResult.getBuffer();
            buffer.writeBytes(valueBuffer);
            valueBuffer.release();
            final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
            logResult(variant, offset, result.getProducedBytes());
            return result;
        } catch (Exception ex) {
            buffer.release();
            throw new EncoderException("Could not encode VARIANT.", ex);
        }
    }

    private Signature determineContentSignature(final DBusType content) throws Exception {
        if (content instanceof DBusBasicType) {
            final String s = String.valueOf(content.getType().getCode().getChar());
            return Signature.valueOf(s);
        } else if (content instanceof DBusContainerType) {
            final DBusContainerType containerType = (DBusContainerType) content;
            return containerType.getSignature();
        } else {
            throw new Exception("can not determine content signature");
        }
    }
}
