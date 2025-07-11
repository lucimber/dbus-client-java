/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class VariantTest {

    @Test
    void createVariantWithString() {
        DBusString string = DBusString.valueOf("Hello, D-Bus!");
        Variant variant = Variant.valueOf(string);
        
        assertEquals(string, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v", variant.getSignature().toString());
    }

    @Test
    void createVariantWithInteger() {
        UInt32 uint32 = UInt32.valueOf(42);
        Variant variant = Variant.valueOf(uint32);
        
        assertEquals(uint32, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v", variant.getSignature().toString());
    }

    @Test
    void createVariantWithObjectPath() {
        ObjectPath objectPath = ObjectPath.valueOf("/com/example/MyObject");
        Variant variant = Variant.valueOf(objectPath);
        
        assertEquals(objectPath, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v", variant.getSignature().toString());
    }

    @Test
    void createVariantWithNestedVariant() {
        DBusString innerString = DBusString.valueOf("nested");
        Variant innerVariant = Variant.valueOf(innerString);
        Variant outerVariant = Variant.valueOf(innerVariant);
        
        assertEquals(innerVariant, outerVariant.getDelegate());
        assertEquals(Type.VARIANT, outerVariant.getType());
        assertEquals("v", outerVariant.getSignature().toString());
    }

    @Test
    void failWithNullValue() {
        assertThrows(NullPointerException.class, () -> Variant.valueOf(null));
    }

    @Test
    void testEquals() {
        DBusString string1 = DBusString.valueOf("test");
        DBusString string2 = DBusString.valueOf("test");
        DBusString string3 = DBusString.valueOf("different");
        
        Variant variant1 = Variant.valueOf(string1);
        Variant variant2 = Variant.valueOf(string2);
        Variant variant3 = Variant.valueOf(string3);
        
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
        
        Variant variant1 = Variant.valueOf(string1);
        Variant variant2 = Variant.valueOf(string2);
        Variant variant3 = Variant.valueOf(string3);
        
        // Equal objects should have equal hash codes
        assertEquals(variant1.hashCode(), variant2.hashCode());
        
        // Different objects should typically have different hash codes
        assertNotEquals(variant1.hashCode(), variant3.hashCode());
    }

    @Test
    void testToStringWithBasicType() {
        // Test with string
        DBusString string = DBusString.valueOf("test");
        Variant variant = Variant.valueOf(string);
        assertEquals("v[s]", variant.toString());
        
        // Test with UInt32
        UInt32 uint32 = UInt32.valueOf(42);
        Variant variantUInt32 = Variant.valueOf(uint32);
        assertEquals("v[u]", variantUInt32.toString());
        
        // Test with ObjectPath
        ObjectPath objectPath = ObjectPath.valueOf("/test");
        Variant variantObjectPath = Variant.valueOf(objectPath);
        assertEquals("v[o]", variantObjectPath.toString());
    }

    @Test
    void testToStringWithContainerType() {
        // Create a variant with another variant (container type)
        DBusString innerString = DBusString.valueOf("nested");
        Variant innerVariant = Variant.valueOf(innerString);
        Variant outerVariant = Variant.valueOf(innerVariant);
        
        // The inner variant should be treated as a container type
        assertEquals("v[v]", outerVariant.toString());
    }

    @Test
    void testGetSignature() {
        DBusString string = DBusString.valueOf("test");
        Variant variant = Variant.valueOf(string);
        
        Signature signature = variant.getSignature();
        assertEquals("v", signature.toString());
        assertEquals(1, signature.getQuantity());
    }

    @Test
    void testGetType() {
        DBusString string = DBusString.valueOf("test");
        Variant variant = Variant.valueOf(string);
        
        assertEquals(Type.VARIANT, variant.getType());
    }

    @Test
    void testGetDelegate() {
        DBusString string = DBusString.valueOf("test");
        Variant variant = Variant.valueOf(string);
        
        assertEquals(string, variant.getDelegate());
        assertSame(string, variant.getDelegate()); // Should be the same reference
    }

    @Test
    void testWithDifferentBasicTypes() {
        // Test with different basic types to ensure proper type handling
        
        // DBusString
        DBusString string = DBusString.valueOf("test");
        Variant stringVariant = Variant.valueOf(string);
        assertEquals("v[s]", stringVariant.toString());
        
        // UInt32
        UInt32 uint32 = UInt32.valueOf(123);
        Variant uint32Variant = Variant.valueOf(uint32);
        assertEquals("v[u]", uint32Variant.toString());
        
        // ObjectPath
        ObjectPath objectPath = ObjectPath.valueOf("/test/path");
        Variant objectPathVariant = Variant.valueOf(objectPath);
        assertEquals("v[o]", objectPathVariant.toString());
    }

    @Test
    void testVariantEquality() {
        // Test that variants are equal if their delegates are equal
        DBusString string1 = DBusString.valueOf("same");
        DBusString string2 = DBusString.valueOf("same");
        DBusString string3 = DBusString.valueOf("different");
        
        Variant variant1 = Variant.valueOf(string1);
        Variant variant2 = Variant.valueOf(string2);
        Variant variant3 = Variant.valueOf(string3);
        
        assertEquals(variant1, variant2);
        assertNotEquals(variant1, variant3);
        
        // Test hash code consistency
        assertEquals(variant1.hashCode(), variant2.hashCode());
    }

    @Test
    void testVariantWithDifferentTypes() {
        // Test that variants containing different types are not equal
        DBusString string = DBusString.valueOf("42");
        UInt32 uint32 = UInt32.valueOf(42);
        
        Variant stringVariant = Variant.valueOf(string);
        Variant uint32Variant = Variant.valueOf(uint32);
        
        assertNotEquals(stringVariant, uint32Variant);
        assertNotEquals(stringVariant.hashCode(), uint32Variant.hashCode());
    }

    @Test
    void testNestedVariants() {
        // Test deeply nested variants
        DBusString innerString = DBusString.valueOf("deep");
        Variant level1 = Variant.valueOf(innerString);
        Variant level2 = Variant.valueOf(level1);
        Variant level3 = Variant.valueOf(level2);
        
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
        Variant variant = Variant.valueOf(string);
        
        // The delegate should be the same reference
        assertSame(string, variant.getDelegate());
        
        // Creating a new variant with the same delegate should be equal
        Variant variant2 = Variant.valueOf(string);
        assertEquals(variant, variant2);
    }

    @Test
    void testVariantWithNullString() {
        // Test variant containing a DBusString with null value
        DBusString nullString = DBusString.valueOf(null);
        Variant variant = Variant.valueOf(nullString);
        
        assertEquals(nullString, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v[s]", variant.toString());
    }

    @Test
    void testVariantWithEmptyString() {
        // Test variant containing a DBusString with empty value
        DBusString emptyString = DBusString.valueOf("");
        Variant variant = Variant.valueOf(emptyString);
        
        assertEquals(emptyString, variant.getDelegate());
        assertEquals(Type.VARIANT, variant.getType());
        assertEquals("v[s]", variant.toString());
    }
}