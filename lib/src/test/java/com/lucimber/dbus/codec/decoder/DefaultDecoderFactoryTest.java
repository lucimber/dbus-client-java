/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.decoder;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeCode;

import static org.junit.jupiter.api.Assertions.*;

class DefaultDecoderFactoryTest {

    private DefaultDecoderFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultDecoderFactory();
    }

    @Test
    void testCanDecodeBasicTypes() {
        assertTrue(factory.canDecode(TypeCode.BOOLEAN));
        assertTrue(factory.canDecode(TypeCode.BYTE));
        assertTrue(factory.canDecode(TypeCode.DOUBLE));
        assertTrue(factory.canDecode(TypeCode.INT16));
        assertTrue(factory.canDecode(TypeCode.INT32));
        assertTrue(factory.canDecode(TypeCode.INT64));
        assertTrue(factory.canDecode(TypeCode.UINT16));
        assertTrue(factory.canDecode(TypeCode.UINT32));
        assertTrue(factory.canDecode(TypeCode.UINT64));
        assertTrue(factory.canDecode(TypeCode.STRING));
        assertTrue(factory.canDecode(TypeCode.OBJECT_PATH));
        assertTrue(factory.canDecode(TypeCode.SIGNATURE));
        assertTrue(factory.canDecode(TypeCode.UNIX_FD));
        assertTrue(factory.canDecode(TypeCode.VARIANT));
    }

    @Test
    void testCanDecodeByType() {
        assertTrue(factory.canDecode(Type.BOOLEAN));
        assertTrue(factory.canDecode(Type.BYTE));
        assertTrue(factory.canDecode(Type.STRING));
        assertTrue(factory.canDecode(Type.INT32));
        assertTrue(factory.canDecode(Type.VARIANT));
    }

    @Test
    void testCannotDecodeContainerTypeCodes() {
        // Container types like ARRAY, DICT_ENTRY, STRUCT don't have direct TypeCode mappings
        // in the basic decoder factory (they require signature information)
        assertFalse(factory.canDecode(TypeCode.ARRAY));
        assertFalse(factory.canDecode(TypeCode.DICT_ENTRY_START));
        assertFalse(factory.canDecode(TypeCode.DICT_ENTRY_END));
        assertFalse(factory.canDecode(TypeCode.STRUCT_START));
        assertFalse(factory.canDecode(TypeCode.STRUCT_END));
    }

    @Test
    void testCreateDecoderForBasicTypes() throws DecoderException {
        for (TypeCode typeCode : TypeCode.values()) {
            if (factory.canDecode(typeCode)) {
                Decoder<ByteBuffer, DBusType> decoder = factory.createDecoder(typeCode);
                assertNotNull(decoder, "Decoder should not be null for type code: " + typeCode);
            }
        }
    }

    @Test
    void testCreateDecoderByType() throws DecoderException {
        Decoder<ByteBuffer, DBusType> decoder = factory.createDecoder(Type.STRING);
        assertNotNull(decoder);
        assertNotNull(decoder);
    }

    @Test
    void testCreateDecoderThrowsExceptionForUnsupportedTypeCode() {
        assertThrows(
                DecoderException.class,
                () -> {
                    factory.createDecoder(TypeCode.ARRAY);
                });
    }

    @Test
    void testCreateDecoderThrowsExceptionForNullTypeCode() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    factory.createDecoder((TypeCode) null);
                });
    }

    @Test
    void testCreateDecoderThrowsExceptionForNullType() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    factory.createDecoder((Type) null);
                });
    }

    @Test
    void testRegisterCustomDecoder() throws DecoderException {
        // Test that we can register a custom decoder
        DefaultDecoderFactory.DecoderCreator customCreator = StringDecoder::new;
        factory.registerDecoder(TypeCode.STRING, customCreator);

        Decoder<ByteBuffer, DBusType> decoder = factory.createDecoder(TypeCode.STRING);
        assertNotNull(decoder);
        assertNotNull(decoder);
    }

    @Test
    void testCanDecodeReturnsFalseForNull() {
        assertFalse(factory.canDecode((TypeCode) null));
        assertFalse(factory.canDecode((Type) null));
    }
}
