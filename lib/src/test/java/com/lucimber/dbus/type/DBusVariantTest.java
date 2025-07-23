/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DBusVariantTest {

    @Test
    void createVariantWithString() {
        DBusString string = DBusString.valueOf("Hello, D-Bus!");
        DBusVariant variant = DBusVariant.valueOf(string);

        assertEquals(string, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v", variant.getSignature().toString());
    }

    @Test
    void createVariantWithInteger() {
        DBusUInt32 uint32 = DBusUInt32.valueOf(42);
        DBusVariant variant = DBusVariant.valueOf(uint32);

        assertEquals(uint32, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v", variant.getSignature().toString());
    }

    @Test
    void createVariantWithObjectPath() {
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/com/example/MyObject");
        DBusVariant variant = DBusVariant.valueOf(objectPath);

        assertEquals(objectPath, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v", variant.getSignature().toString());
    }

    @Test
    void createVariantWithNestedVariant() {
        DBusString innerString = DBusString.valueOf("nested");
        DBusVariant innerVariant = DBusVariant.valueOf(innerString);
        DBusVariant outerVariant = DBusVariant.valueOf(innerVariant);

        assertEquals(innerVariant, outerVariant.getDelegate());
        assertEquals(Type.VARIANT, outerVariant.getType());
        assertEquals("v", outerVariant.getSignature().toString());
    }

    @Test
    void failWithNullValue() {
        assertThrows(NullPointerException.class, () -> DBusVariant.valueOf(null));
    }

    @Test
    void testEquals() {
        DBusString string1 = DBusString.valueOf("test");
        DBusString string2 = DBusString.valueOf("test");
        DBusString string3 = DBusString.valueOf("different");

        DBusVariant variant1 = DBusVariant.valueOf(string1);
        DBusVariant variant2 = DBusVariant.valueOf(string2);
        DBusVariant variant3 = DBusVariant.valueOf(string3);

        // Test equality based on delegate
        assertEquals(variant1, variant2);
        assertNotEquals(variant1, variant3);

        // Test self-equality
        assertEquals(variant1, variant1);

        // Test against null and different types
        assertNotEquals(variant1, null);
        assertNotEquals(variant1, string1); // Different type
        assertNotEquals(variant1, "test"); // Different type
    }

    @Test
    void testHashCode() {
        DBusString string1 = DBusString.valueOf("test");
        DBusString string2 = DBusString.valueOf("test");
        DBusString string3 = DBusString.valueOf("different");

        DBusVariant variant1 = DBusVariant.valueOf(string1);
        DBusVariant variant2 = DBusVariant.valueOf(string2);
        DBusVariant variant3 = DBusVariant.valueOf(string3);

        // Equal objects should have equal hash codes
        assertEquals(variant1.hashCode(), variant2.hashCode());

        // Different objects should typically have different hash codes
        assertNotEquals(variant1.hashCode(), variant3.hashCode());
    }

    @Test
    void testToStringWithBasicType() {
        // Test with string
        DBusString string = DBusString.valueOf("test");
        DBusVariant variant = DBusVariant.valueOf(string);
        assertEquals("v[s]", variant.toString());

        // Test with UInt32
        DBusUInt32 uint32 = DBusUInt32.valueOf(42);
        DBusVariant variantUInt32 = DBusVariant.valueOf(uint32);
        assertEquals("v[u]", variantUInt32.toString());

        // Test with ObjectPath
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/test");
        DBusVariant variantObjectPath = DBusVariant.valueOf(objectPath);
        assertEquals("v[o]", variantObjectPath.toString());
    }

    @Test
    void testToStringWithContainerType() {
        // Create a variant with another variant (container type)
        DBusString innerString = DBusString.valueOf("nested");
        DBusVariant innerVariant = DBusVariant.valueOf(innerString);
        DBusVariant outerVariant = DBusVariant.valueOf(innerVariant);

        // The inner variant should be treated as a container type
        assertEquals("v[v]", outerVariant.toString());
    }

    @Test
    void testGetSignature() {
        DBusString string = DBusString.valueOf("test");
        DBusVariant variant = DBusVariant.valueOf(string);

        DBusSignature signature = variant.getSignature();
        assertEquals("v", signature.toString());
        assertEquals(1, signature.getQuantity());
    }

    @Test
    void testGetType() {
        DBusString string = DBusString.valueOf("test");
        DBusVariant variant = DBusVariant.valueOf(string);

        assertEquals(Type.VARIANT, variant.getType());
    }

    @Test
    void testGetDelegate() {
        DBusString string = DBusString.valueOf("test");
        DBusVariant variant = DBusVariant.valueOf(string);

        assertEquals(string, variant.getDelegate());
        assertSame(string, variant.getDelegate()); // Should be the same reference
    }

    @Test
    void testWithDifferentBasicTypes() {
        // Test with different basic types to ensure proper type handling

        // DBusString
        DBusString string = DBusString.valueOf("test");
        DBusVariant stringVariant = DBusVariant.valueOf(string);
        assertEquals("v[s]", stringVariant.toString());

        // UInt32
        DBusUInt32 uint32 = DBusUInt32.valueOf(123);
        DBusVariant uint32Variant = DBusVariant.valueOf(uint32);
        assertEquals("v[u]", uint32Variant.toString());

        // ObjectPath
        DBusObjectPath objectPath = DBusObjectPath.valueOf("/test/path");
        DBusVariant objectPathVariant = DBusVariant.valueOf(objectPath);
        assertEquals("v[o]", objectPathVariant.toString());
    }

    @Test
    void testVariantEquality() {
        // Test that variants are equal if their delegates are equal
        DBusString string1 = DBusString.valueOf("same");
        DBusString string2 = DBusString.valueOf("same");
        DBusString string3 = DBusString.valueOf("different");

        DBusVariant variant1 = DBusVariant.valueOf(string1);
        DBusVariant variant2 = DBusVariant.valueOf(string2);
        DBusVariant variant3 = DBusVariant.valueOf(string3);

        assertEquals(variant1, variant2);
        assertNotEquals(variant1, variant3);

        // Test hash code consistency
        assertEquals(variant1.hashCode(), variant2.hashCode());
    }

    @Test
    void testVariantWithDifferentTypes() {
        // Test that variants containing different types are not equal
        DBusString string = DBusString.valueOf("42");
        DBusUInt32 uint32 = DBusUInt32.valueOf(42);

        DBusVariant stringVariant = DBusVariant.valueOf(string);
        DBusVariant uint32Variant = DBusVariant.valueOf(uint32);

        assertNotEquals(stringVariant, uint32Variant);
        assertNotEquals(stringVariant.hashCode(), uint32Variant.hashCode());
    }

    @Test
    void testNestedVariants() {
        // Test deeply nested variants
        DBusString innerString = DBusString.valueOf("deep");
        DBusVariant level1 = DBusVariant.valueOf(innerString);
        DBusVariant level2 = DBusVariant.valueOf(level1);
        DBusVariant level3 = DBusVariant.valueOf(level2);

        assertEquals(level2, level3.getDelegate());
        assertEquals(level1, level2.getDelegate());
        assertEquals(innerString, level1.getDelegate());

        // All levels should have the same type and signature
        assertEquals(Type.VARIANT, level1.getType());
        assertEquals(Type.VARIANT, level2.getType());
        assertEquals(Type.VARIANT, level3.getType());

        assertEquals("v", level1.getSignature().toString());
        assertEquals("v", level2.getSignature().toString());
        assertEquals("v", level3.getSignature().toString());
    }

    @Test
    void testVariantImmutability() {
        DBusString string = DBusString.valueOf("test");
        DBusVariant variant = DBusVariant.valueOf(string);

        // The delegate should be the same reference
        assertSame(string, variant.getDelegate());

        // Creating a new variant with the same delegate should be equal
        DBusVariant variant2 = DBusVariant.valueOf(string);
        assertEquals(variant, variant2);
    }

    @Test
    void testVariantWithNullStringRejection() {
        // Test that null strings are properly rejected per D-Bus specification
        assertThrows(
                NullPointerException.class,
                () -> {
                    DBusString.valueOf(null);
                });
    }

    @Test
    void testVariantWithEmptyString() {
        // Test variant containing a DBusString with empty value
        DBusString emptyString = DBusString.valueOf("");
        DBusVariant variant = DBusVariant.valueOf(emptyString);

        assertEquals(emptyString, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v[s]", variant.toString());
    }
}
