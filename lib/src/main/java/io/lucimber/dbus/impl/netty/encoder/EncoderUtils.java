package io.lucimber.dbus.impl.netty.encoder;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.type.DBusArray;
import io.lucimber.dbus.type.DBusBasicType;
import io.lucimber.dbus.type.DBusBoolean;
import io.lucimber.dbus.type.DBusByte;
import io.lucimber.dbus.type.DBusContainerType;
import io.lucimber.dbus.type.DBusDouble;
import io.lucimber.dbus.type.Int32;
import io.lucimber.dbus.type.Int64;
import io.lucimber.dbus.type.Int16;
import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.Dict;
import io.lucimber.dbus.type.DictEntry;
import io.lucimber.dbus.type.ObjectPath;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.Struct;
import io.lucimber.dbus.type.Type;
import io.lucimber.dbus.type.TypeAlignment;
import io.lucimber.dbus.type.UnixFd;
import io.lucimber.dbus.type.UInt32;
import io.lucimber.dbus.type.UInt64;
import io.lucimber.dbus.type.UInt16;
import io.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public final class EncoderUtils {

    private EncoderUtils() {
        // Utility class
    }

    static int applyPadding(final ByteBuf buffer, final int offset, final Type type) {
        final int padding = calculateAlignmentPadding(type.getAlignment(), offset);
        if (padding > 0) {
            buffer.writeZero(padding);
        }
        return padding;
    }

    static int calculateAlignmentPadding(final TypeAlignment alignment, final int offset) {
        final int remainder = offset % alignment.getAlignment();
        if (remainder > 0) {
            return alignment.getAlignment() - remainder;
        } else {
            return 0;
        }
    }

    public static EncoderResult<ByteBuf> encode(final DBusType value, final int offset,
                                                final ByteOrder order) throws EncoderException {
        if (value instanceof DBusContainerType) {
            return encodeContainerType((DBusContainerType) value, offset, order);
        } else {
            return encodeBasicType((DBusBasicType) value, offset, order);
        }
    }

    static EncoderResult<ByteBuf> encodeBasicType(final DBusBasicType value, final int offset,
                                                  final ByteOrder order) throws EncoderException {
        final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        if (value instanceof DBusBoolean) {
            return new BooleanEncoder(allocator, order).encode((DBusBoolean) value, offset);
        } else if (value instanceof DBusByte) {
            return new ByteEncoder(allocator).encode((DBusByte) value, offset);
        } else if (value instanceof DBusDouble) {
            return new DoubleEncoder(allocator, order).encode((DBusDouble) value, offset);
        } else if (value instanceof Int32) {
            return new Int32Encoder(allocator, order).encode((Int32) value, offset);
        } else if (value instanceof Int64) {
            return new Int64Encoder(allocator, order).encode((Int64) value, offset);
        } else if (value instanceof ObjectPath) {
            return new ObjectPathEncoder(allocator, order).encode((ObjectPath) value, offset);
        } else if (value instanceof Int16) {
            return new Int16Encoder(allocator, order).encode((Int16) value, offset);
        } else if (value instanceof Signature) {
            return new SignatureEncoder(allocator).encode((Signature) value, offset);
        } else if (value instanceof DBusString) {
            return new StringEncoder(allocator, order).encode((DBusString) value, offset);
        } else if (value instanceof UnixFd) {
            return new UnixFdEncoder(allocator, order).encode((UnixFd) value, offset);
        } else if (value instanceof UInt32) {
            return new UInt32Encoder(allocator, order).encode((UInt32) value, offset);
        } else if (value instanceof UInt64) {
            return new UInt64Encoder(allocator, order).encode((UInt64) value, offset);
        } else if (value instanceof UInt16) {
            return new UInt16Encoder(allocator, order).encode((UInt16) value, offset);
        } else {
            throw new EncoderException("Unknown D-Bus data type: " + value);
        }
    }

    static EncoderResult<ByteBuf> encodeContainerType(final DBusContainerType value, final int offset,
                                                      final ByteOrder order) throws EncoderException {
        final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        try {
            final Signature signature = value.getSignature();
            if (signature.isArray()) {
                @SuppressWarnings("unchecked")
                final DBusArray<DBusType> array = (DBusArray<DBusType>) value;
                return new ArrayEncoder<>(allocator, order, signature).encode(array, offset);
            } else if (signature.isDictionary()) {
                @SuppressWarnings("unchecked")
                final Dict<DBusBasicType, DBusType> dict = (Dict<DBusBasicType, DBusType>) value;
                return new DictEncoder<>(allocator, order, signature).encode(dict, offset);
            } else if (signature.isDictionaryEntry()) {
                @SuppressWarnings("unchecked")
                final DictEntry<DBusBasicType, DBusType> dictEntry = (DictEntry<DBusBasicType, DBusType>) value;
                return new DictEntryEncoder<>(allocator, order, signature).encode(dictEntry, offset);
            } else if (signature.isStruct()) {
                final Struct struct = (Struct) value;
                return new StructEncoder(allocator, order, signature).encode(struct, offset);
            } else if (signature.isVariant()) {
                final Variant variant = (Variant) value;
                return new VariantEncoder(allocator, order).encode(variant, offset);
            }
        } catch (ClassCastException e) {
            throw new EncoderException("Mismatch between signature and value.", e);
        }
        throw new EncoderException("Unknown container type");
    }
}
