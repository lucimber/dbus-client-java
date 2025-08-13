/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultEncoderFactoryTest {

    private DefaultEncoderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultEncoderFactory();
    }

    @Test
    void testCanEncodeBasicTypes() {
        assertTrue(factory.canEncode(Type.BOOLEAN));
        assertTrue(factory.canEncode(Type.BYTE));
        assertTrue(factory.canEncode(Type.DOUBLE));
        assertTrue(factory.canEncode(Type.INT16));
        assertTrue(factory.canEncode(Type.INT32));
        assertTrue(factory.canEncode(Type.INT64));
        assertTrue(factory.canEncode(Type.UINT16));
        assertTrue(factory.canEncode(Type.UINT32));
        assertTrue(factory.canEncode(Type.UINT64));
        assertTrue(factory.canEncode(Type.STRING));
        assertTrue(factory.canEncode(Type.OBJECT_PATH));
        assertTrue(factory.canEncode(Type.SIGNATURE));
        assertTrue(factory.canEncode(Type.UNIX_FD));
    }

    @Test
    void testCanEncodeContainerTypes() {
        // Container types requiring signatures are not supported by basic factory
        assertFalse(factory.canEncode(Type.ARRAY));
        assertFalse(factory.canEncode(Type.DICT_ENTRY));
        assertFalse(factory.canEncode(Type.STRUCT));

        // VARIANT is supported as it doesn't require external signature
        assertTrue(factory.canEncode(Type.VARIANT));
    }

    @Test
    void testCreateEncoderForBasicTypes() throws EncoderException {
        for (Type type : Type.values()) {
            if (factory.canEncode(type)) {
                Encoder<DBusType, ByteBuffer> encoder =
                        factory.createEncoder(type, ByteOrder.LITTLE_ENDIAN);
                assertNotNull(encoder, "Encoder should not be null for type: " + type);
            }
        }
    }

    @Test
    void testCreateEncoderWithDifferentByteOrders() throws EncoderException {
        Encoder<DBusType, ByteBuffer> littleEndianEncoder =
                factory.createEncoder(Type.INT32, ByteOrder.LITTLE_ENDIAN);
        Encoder<DBusType, ByteBuffer> bigEndianEncoder =
                factory.createEncoder(Type.INT32, ByteOrder.BIG_ENDIAN);

        assertNotNull(littleEndianEncoder);
        assertNotNull(bigEndianEncoder);
    }

    @Test
    void testCreateEncoderThrowsExceptionForUnsupportedType() {
        // Test with unsupported container types
        assertThrows(
                EncoderException.class,
                () -> {
                    factory.createEncoder(Type.ARRAY, ByteOrder.LITTLE_ENDIAN);
                });

        assertThrows(
                EncoderException.class,
                () -> {
                    factory.createEncoder(Type.STRUCT, ByteOrder.LITTLE_ENDIAN);
                });

        // Test with null
        assertThrows(
                NullPointerException.class,
                () -> {
                    factory.createEncoder(null, ByteOrder.LITTLE_ENDIAN);
                });
    }

    @Test
    void testRegisterCustomEncoder() throws EncoderException {
        // Test that we can register a custom encoder (though all types are already supported)
        DefaultEncoderFactory.EncoderCreator customCreator = order -> new StringEncoder(order);
        factory.registerEncoder(Type.STRING, customCreator);

        Encoder<DBusType, ByteBuffer> encoder =
                factory.createEncoder(Type.STRING, ByteOrder.LITTLE_ENDIAN);
        assertNotNull(encoder);
        assertNotNull(encoder);
    }
}
