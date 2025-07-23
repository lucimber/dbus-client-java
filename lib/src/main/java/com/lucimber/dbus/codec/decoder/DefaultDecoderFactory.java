/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeCode;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of DecoderFactory that provides decoders for all standard D-Bus types.
 *
 * <p>This factory uses a registry-based approach to create decoders, making it easy to extend with
 * new types and improving performance by avoiding switch statements.
 */
public final class DefaultDecoderFactory implements DecoderFactory {

    private final Map<TypeCode, DecoderCreator> decoderRegistry;

    /** Creates a new DefaultDecoderFactory with all standard D-Bus decoders registered. */
    public DefaultDecoderFactory() {
        this.decoderRegistry = new EnumMap<>(TypeCode.class);
        registerStandardDecoders();
    }

    private void registerStandardDecoders() {
        // Basic types
        decoderRegistry.put(TypeCode.BOOLEAN, BooleanDecoder::new);
        decoderRegistry.put(TypeCode.BYTE, ByteDecoder::new);
        decoderRegistry.put(TypeCode.DOUBLE, DoubleDecoder::new);
        decoderRegistry.put(TypeCode.INT16, Int16Decoder::new);
        decoderRegistry.put(TypeCode.INT32, Int32Decoder::new);
        decoderRegistry.put(TypeCode.INT64, Int64Decoder::new);
        decoderRegistry.put(TypeCode.UINT16, UInt16Decoder::new);
        decoderRegistry.put(TypeCode.UINT32, UInt32Decoder::new);
        decoderRegistry.put(TypeCode.UINT64, UInt64Decoder::new);
        decoderRegistry.put(TypeCode.STRING, StringDecoder::new);
        decoderRegistry.put(TypeCode.OBJECT_PATH, ObjectPathDecoder::new);
        decoderRegistry.put(TypeCode.SIGNATURE, SignatureDecoder::new);
        decoderRegistry.put(TypeCode.UNIX_FD, UnixFdDecoder::new);
        decoderRegistry.put(TypeCode.VARIANT, VariantDecoder::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Decoder<ByteBuffer, DBusType> createDecoder(TypeCode typeCode) throws DecoderException {
        Objects.requireNonNull(typeCode, "typeCode must not be null");

        DecoderCreator creator = decoderRegistry.get(typeCode);
        if (creator == null) {
            throw new DecoderException("No decoder available for type code: " + typeCode);
        }

        return (Decoder<ByteBuffer, DBusType>) creator.create();
    }

    @Override
    public Decoder<ByteBuffer, DBusType> createDecoder(Type type) throws DecoderException {
        Objects.requireNonNull(type, "type must not be null");

        TypeCode typeCode = type.getCode();
        return createDecoder(typeCode);
    }

    @Override
    public boolean canDecode(TypeCode typeCode) {
        return decoderRegistry.containsKey(typeCode);
    }

    @Override
    public boolean canDecode(Type type) {
        return type != null && canDecode(type.getCode());
    }

    /**
     * Registers a custom decoder creator for a specific type code.
     *
     * @param typeCode the D-Bus type code
     * @param creator the decoder creator function
     */
    public void registerDecoder(TypeCode typeCode, DecoderCreator creator) {
        Objects.requireNonNull(typeCode, "typeCode must not be null");
        Objects.requireNonNull(creator, "creator must not be null");
        decoderRegistry.put(typeCode, creator);
    }

    /** Functional interface for creating decoders. */
    @FunctionalInterface
    public interface DecoderCreator {
        Decoder<ByteBuffer, ?> create() throws DecoderException;
    }
}
