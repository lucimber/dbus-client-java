package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.Dict;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DictEntry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * An encoder which encodes a dictionary to the D-Bus marshalling format.
 *
 * @param <KeyT>   The data type of the key.
 * @param <ValueT> The data type of the value.
 * @see Encoder
 * @see Dict
 */
public final class DictEncoder<KeyT extends DBusBasicType, ValueT extends DBusType>
        implements Encoder<Dict<KeyT, ValueT>, ByteBuf> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_DATA_MARSHALLING);

    private final ByteBufAllocator allocator;
    private final ByteOrder order;
    private final Signature signature;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param allocator The buffer factory.
     * @param order     a {@link ByteOrder}
     * @param signature a {@link Signature}
     */
    public DictEncoder(final ByteBufAllocator allocator, final ByteOrder order, final Signature signature) {
        this.allocator = Objects.requireNonNull(allocator, "allocator must not be null");
        this.order = Objects.requireNonNull(order, "order must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
    }

    private static void logResult(final Signature signature, final int offset, final int padding,
                                  final int producedBytes) {
        LoggerUtils.debug(LOGGER, MARKER, () -> {
            final String s = "ARRAY: %s; Offset: %d; Padding: %d; Produced bytes: %d;";
            return String.format(s, signature, offset, padding, producedBytes);
        });
    }

    @Override
    public EncoderResult<ByteBuf> encode(final Dict<KeyT, ValueT> dict, final int offset)
            throws EncoderException {
        Objects.requireNonNull(dict, "dict must not be null");
        final ByteBuf buffer = allocator.buffer();
        try {
            int producedBytes = 0;
            // Determine alignment for array
            final int padding = EncoderUtils.applyPadding(buffer, offset, Type.ARRAY);
            producedBytes += padding;
            // Determine alignment for dict-entry
            final int sizeBytes = 4;
            final int typeOffset = offset + padding + sizeBytes;
            final int typePadding = EncoderUtils.calculateAlignmentPadding(Type.DICT_ENTRY.getAlignment(), typeOffset);
            // Iterate over each dict-entry
            final Encoder<DictEntry<KeyT, ValueT>, ByteBuf> entryEncoder =
                    new DictEntryEncoder<>(allocator, order, signature.subContainer());
            final ByteBuf elementsBuffer = allocator.buffer();
            int interimProducedBytes = 0;
            for (DictEntry<KeyT, ValueT> entry : dict.dictionaryEntrySet()) {
                final int entryOffset = offset + padding + sizeBytes + typePadding + interimProducedBytes;
                final EncoderResult<ByteBuf> entryResult = entryEncoder.encode(entry, entryOffset);
                interimProducedBytes += entryResult.getProducedBytes();
                final ByteBuf entryBuffer = entryResult.getBuffer();
                elementsBuffer.writeBytes(entryBuffer);
                entryBuffer.release();
            }
            // Array size
            final int length = elementsBuffer.readableBytes();
            final int sizeOffset = offset + padding;
            final EncoderResult<ByteBuf> sizeResult = encodeArrayLength(length, sizeOffset);
            final ByteBuf sizeBuffer = sizeResult.getBuffer();
            buffer.writeBytes(sizeBuffer);
            sizeBuffer.release();
            producedBytes += sizeResult.getProducedBytes();
            if (typePadding > 0) {
                buffer.writeZero(typePadding);
                producedBytes += typePadding;
            }
            // Write elements
            buffer.writeBytes(elementsBuffer);
            elementsBuffer.release();
            producedBytes += interimProducedBytes;
            final EncoderResult<ByteBuf> result = new EncoderResultImpl<>(producedBytes, buffer);
            logResult(signature, offset, padding + typePadding, result.getProducedBytes());
            return result;
        } catch (Exception ex) {
            buffer.release();
            throw new EncoderException("Could not encode ARRAY of DICT_ENTRY.", ex);
        }
    }

    private EncoderResult<ByteBuf> encodeArrayLength(final int length, final int offset) throws EncoderException {
        final Encoder<UInt32, ByteBuf> encoder = new UInt32Encoder(allocator, order);
        return encoder.encode(UInt32.valueOf(length), offset);
    }
}
