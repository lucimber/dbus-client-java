/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Factory interface for creating D-Bus encoders.
 *
 * <p>This factory provides a centralized way to create encoders for different D-Bus types,
 * eliminating the need for switch statements and improving extensibility.
 */
public interface EncoderFactory {

    /**
     * Creates an encoder for the specified D-Bus type.
     *
     * @param type the D-Bus type to encode
     * @param order the byte order to use for encoding
     * @return an encoder capable of encoding the specified type
     * @throws EncoderException if no encoder is available for the type
     */
    Encoder<DBusType, ByteBuffer> createEncoder(Type type, ByteOrder order) throws EncoderException;

    /**
     * Checks if an encoder is available for the specified type.
     *
     * @param type the D-Bus type to check
     * @return true if an encoder is available, false otherwise
     */
    boolean canEncode(Type type);
}
