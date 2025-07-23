/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeCode;
import java.nio.ByteBuffer;

/**
 * Factory interface for creating D-Bus decoders.
 *
 * <p>This factory provides a centralized way to create decoders for different D-Bus types,
 * eliminating the need for switch statements and improving extensibility.
 */
public interface DecoderFactory {

    /**
     * Creates a decoder for the specified D-Bus type code.
     *
     * @param typeCode the D-Bus type code to decode
     * @return a decoder capable of decoding the specified type
     * @throws DecoderException if no decoder is available for the type
     */
    Decoder<ByteBuffer, DBusType> createDecoder(TypeCode typeCode) throws DecoderException;

    /**
     * Creates a decoder for the specified D-Bus type.
     *
     * @param type the D-Bus type to decode
     * @return a decoder capable of decoding the specified type
     * @throws DecoderException if no decoder is available for the type
     */
    Decoder<ByteBuffer, DBusType> createDecoder(Type type) throws DecoderException;

    /**
     * Checks if a decoder is available for the specified type code.
     *
     * @param typeCode the D-Bus type code to check
     * @return true if a decoder is available, false otherwise
     */
    boolean canDecode(TypeCode typeCode);

    /**
     * Checks if a decoder is available for the specified type.
     *
     * @param type the D-Bus type to check
     * @return true if a decoder is available, false otherwise
     */
    boolean canDecode(Type type);
}
