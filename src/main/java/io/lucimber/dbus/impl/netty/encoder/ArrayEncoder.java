package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusArray;
import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.Type;
import io.lucimber.dbus.type.TypeUtils;
import io.lucimber.dbus.type.UInt32;
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
 * An encoder which encodes an array to the D-Bus marshalling format.
 *
 * @param <E> The element's data type.
 * @see Encoder
 * @see DBusArray
 */
public final class ArrayEncoder<E extends DBusType> implements Encoder<DBusArray<E>, ByteBuf> {

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
     * @param signature a {@link Signature}; must describe an array
     */
    public ArrayEncoder(final ByteBufAllocator allocator, final ByteOrder order, final Signature signature) {
        this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        if (!signature.isArray()) {
            throw new IllegalArgumentException("signature does not describe an array");
        }
    }

    private static void logResult(final Signature signature, final int offset, final int padding,
                                  final int producedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "ARRAY: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
            return String.format(s, signature, offset, padding, producedBytes);
        });
    }

    @Override
    public EncoderResult<ByteBuf> encode(final DBusArray<E> array, final int offset) throws EncoderException {
        final ByteBuf buffer = allocator.buffer();
        try {
            int producedBytes = 0;
            // Alignment of the array itself
            final int padding = EncoderUtils.applyPadding(buffer, offset, Type.ARRAY);
            producedBytes += padding;
            // Determine padding of array's type
            final char c = signature.subContainer().toString().charAt(0);
            final Type type = TypeUtils.getTypeFromChar(c)
                    .orElseThrow(() -> new Exception("can not map char to type: " + c));
            final int sizeBytes = 4;
            final int typeOffset = offset + padding + sizeBytes;
            final int typePadding = EncoderUtils.calculateAlignmentPadding(type.getAlignment(), typeOffset);
            // Marshall all elements to interim buffer
            final int elementsOffset = offset + padding + sizeBytes + typePadding;
            final EncoderResult<ByteBuf> elementsResult = encodeElements(array, elementsOffset);
            // Array size
            final int length = elementsResult.getProducedBytes();
            final EncoderResult<ByteBuf> sizeResult = encodeArrayLength(length);
            final ByteBuf sizeBuffer = sizeResult.getBuffer();
            buffer.writeBytes(sizeBuffer);
            sizeBuffer.release();
            producedBytes += sizeResult.getProducedBytes();
            if (typePadding > 0) {
                buffer.writeZero(typePadding);
                producedBytes += typePadding;
            }
            // Write interim buffer of elements
            producedBytes += elementsResult.getProducedBytes();
            final ByteBuf elementsBuffer = elementsResult.getBuffer();
            buffer.writeBytes(elementsBuffer);
            elementsBuffer.release();
            final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
            logResult(signature, offset, padding + typePadding, result.getProducedBytes());
            return result;
        } catch (Exception ex) {
            buffer.release();
            final String failureMsg =
                    String.format("Could not encode the following DBus type: %s", array.getSignature());
            throw new EncoderException(failureMsg, ex);
        }
    }

    private EncoderResult<ByteBuf> encodeArrayLength(final int length) throws EncoderException {
        final Encoder<UInt32, ByteBuf> encoder = new UInt32Encoder(allocator, order);
        return encoder.encode(UInt32.valueOf(length), 0);
    }

    private EncoderResult<ByteBuf> encodeElements(final DBusArray<E> elements, final int byteOffset) {
        final ByteBuf buffer = allocator.buffer();
        try {
            int producedBytes = 0;
            for (DBusType e : elements) {
                final int interimByteOffset = byteOffset + producedBytes;
                final EncoderResult<ByteBuf> result = EncoderUtils.encode(e, interimByteOffset, order);
                final ByteBuf tmpBuffer = result.getBuffer();
                buffer.writeBytes(tmpBuffer);
                tmpBuffer.release();
                producedBytes += result.getProducedBytes();
            }
            return new EncoderResultImpl<>(producedBytes, buffer);
        } catch (Throwable t) {
            buffer.release();
            throw t;
        }
    }
}
