/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusContainerType;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusDictEntry;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusStruct;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusVariant;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeAlignment;

/** Various methods used by the ByteBuffer-based implementations of the encoders. */
public final class EncoderUtils {

    private static final EncoderFactory ENCODER_FACTORY = new DefaultEncoderFactory();

    private EncoderUtils() {
        // Utility class
    }

    static int calculateAlignmentPadding(TypeAlignment alignment, int offset) {
        int remainder = offset % alignment.getAlignment();
        return (remainder == 0) ? 0 : (alignment.getAlignment() - remainder);
    }

    /**
     * Encodes a DBusType into its binary representation using ByteBuffer.
     *
     * @param value the value to encode
     * @param offset the offset where the value will be written
     * @param order byte order to use
     * @return an EncoderResult with the encoded buffer and byte count
     * @throws EncoderException if the value cannot be encoded
     */
    public static EncoderResult<ByteBuffer> encode(DBusType value, int offset, ByteOrder order)
            throws EncoderException {
        Objects.requireNonNull(value, "value must not be null");
        if (value instanceof DBusContainerType container) {
            return encodeContainerType(container, offset, order);
        } else if (value instanceof DBusBasicType basic) {
            return encodeBasicType(basic, offset, order);
        } else {
            throw new EncoderException("Unknown DBusType: " + value.getClass().getName());
        }
    }

    private static EncoderResult<ByteBuffer> encodeBasicType(
            DBusBasicType value, int offset, ByteOrder order) throws EncoderException {
        Type type = value.getType();
        Encoder<DBusType, ByteBuffer> encoder = ENCODER_FACTORY.createEncoder(type, order);
        return encoder.encode(value, offset);
    }

    @SuppressWarnings("unchecked")
    private static EncoderResult<ByteBuffer> encodeContainerType(
            DBusContainerType value, int offset, ByteOrder order) throws EncoderException {
        DBusSignature signature = value.getSignature();
        if (signature.isArray()) {
            return new ArrayEncoder<>(order, signature).encode((DBusArray<DBusType>) value, offset);
        } else if (signature.isDictionary()) {
            return new DictEncoder<>(order, signature)
                    .encode((DBusDict<DBusBasicType, DBusType>) value, offset);
        } else if (signature.isDictionaryEntry()) {
            return new DictEntryEncoder<>(order, signature)
                    .encode((DBusDictEntry<DBusBasicType, DBusType>) value, offset);
        } else if (signature.isStruct()) {
            return new StructEncoder(order, signature).encode((DBusStruct) value, offset);
        } else if (signature.isVariant()) {
            return new VariantEncoder(order).encode((DBusVariant) value, offset);
        } else {
            throw new EncoderException("Unsupported container type for signature: " + signature);
        }
    }
}
