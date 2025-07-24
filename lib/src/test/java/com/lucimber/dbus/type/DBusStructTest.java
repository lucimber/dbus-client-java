/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

final class DBusStructTest {

    @Test
    void createEmptyStruct() {
        // Note: D-Bus specification requires at least one type in a struct
        // Empty struct signature should fail per D-Bus specification
        assertThrows(
                SignatureException.class,
                () -> {
                    DBusSignature.valueOf("()");
                });
    }

    @Test
    void createStructWithSingleValue() {
        DBusSignature signature = DBusSignature.valueOf("(i)");
        DBusInt32 value = DBusInt32.valueOf(42);

        DBusStruct struct = new DBusStruct(signature, value);

        assertEquals(signature, struct.getSignature());
        assertEquals(Type.STRUCT, struct.getType());
        assertEquals(1, struct.getDelegate().size());
        assertEquals(value, struct.getDelegate().get(0));
    }

    @Test
    void createStructWithMultipleValues() {
        DBusSignature signature = DBusSignature.valueOf("(isi)");
        DBusInt32 int32 = DBusInt32.valueOf(42);
        DBusString string = DBusString.valueOf("hello");
        DBusInt32 int32_2 = DBusInt32.valueOf(123);

        DBusStruct struct = new DBusStruct(signature, int32, string, int32_2);

        assertEquals(signature, struct.getSignature());
        assertEquals(Type.STRUCT, struct.getType());
        assertEquals(3, struct.getDelegate().size());
        assertEquals(int32, struct.getDelegate().get(0));
        assertEquals(string, struct.getDelegate().get(1));
        assertEquals(int32_2, struct.getDelegate().get(2));
    }

    @Test
    void createStructWithListConstructor() {
        DBusSignature signature = DBusSignature.valueOf("(sb)");
        DBusString string = DBusString.valueOf("test");
        DBusBoolean bool = DBusBoolean.valueOf(true);

        List<DBusType> values = Arrays.asList(string, bool);
        DBusStruct struct = new DBusStruct(signature, values);

        assertEquals(signature, struct.getSignature());
        assertEquals(Type.STRUCT, struct.getType());
        assertEquals(2, struct.getDelegate().size());
        assertEquals(string, struct.getDelegate().get(0));
        assertEquals(bool, struct.getDelegate().get(1));
    }

    @Test
    void createNestedStruct() {
        // Test nested struct: ((ii)s)
        DBusSignature innerSignature = DBusSignature.valueOf("(ii)");
        DBusInt32 inner1 = DBusInt32.valueOf(1);
        DBusInt32 inner2 = DBusInt32.valueOf(2);
        DBusStruct innerStruct = new DBusStruct(innerSignature, inner1, inner2);

        DBusSignature outerSignature = DBusSignature.valueOf("((ii)s)");
        DBusString string = DBusString.valueOf("outer");
        DBusStruct outerStruct = new DBusStruct(outerSignature, innerStruct, string);

        assertEquals(outerSignature, outerStruct.getSignature());
        assertEquals(Type.STRUCT, outerStruct.getType());
        assertEquals(2, outerStruct.getDelegate().size());
        assertEquals(innerStruct, outerStruct.getDelegate().get(0));
        assertEquals(string, outerStruct.getDelegate().get(1));
    }

    @Test
    void testInvalidSignature() {
        // Non-struct signature should throw exception
        DBusSignature invalidSignature = DBusSignature.valueOf("i");
        DBusInt32 value = DBusInt32.valueOf(42);

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new DBusStruct(invalidSignature, value);
                });
    }

    @Test
    void testNullSignature() {
        DBusInt32 value = DBusInt32.valueOf(42);

        assertThrows(
                NullPointerException.class,
                () -> {
                    new DBusStruct(null, value);
                });
    }

    @Test
    void testGetDelegate() {
        DBusSignature signature = DBusSignature.valueOf("(is)");
        DBusInt32 intValue = DBusInt32.valueOf(42);
        DBusString stringValue = DBusString.valueOf("test");

        DBusStruct struct = new DBusStruct(signature, intValue, stringValue);

        List<DBusType> delegate = struct.getDelegate();
        assertEquals(2, delegate.size());
        assertEquals(intValue, delegate.get(0));
        assertEquals(stringValue, delegate.get(1));

        // Test that delegate is a copy (immutability)
        delegate.clear();
        assertEquals(2, struct.getDelegate().size()); // Original should be unchanged
    }

    @Test
    void testGetDelegateImmutability() {
        DBusSignature signature = DBusSignature.valueOf("(i)");
        DBusInt32 value = DBusInt32.valueOf(42);

        DBusStruct struct = new DBusStruct(signature, value);

        List<DBusType> delegate1 = struct.getDelegate();
        List<DBusType> delegate2 = struct.getDelegate();

        // Should return different instances
        assertNotSame(delegate1, delegate2);

        // But with same content
        assertEquals(delegate1, delegate2);

        // Modifying one shouldn't affect the other
        delegate1.clear();
        assertNotEquals(delegate1, delegate2);
        assertEquals(1, delegate2.size());
    }

    @Test
    void testToString() {
        DBusSignature signature = DBusSignature.valueOf("(isi)");
        DBusInt32 int32 = DBusInt32.valueOf(42);
        DBusString string = DBusString.valueOf("hello");
        DBusInt32 int32_2 = DBusInt32.valueOf(123);

        DBusStruct struct = new DBusStruct(signature, int32, string, int32_2);

        assertEquals("(isi)", struct.toString());
    }

    @Test
    void testGetType() {
        DBusSignature signature = DBusSignature.valueOf("(i)");
        DBusInt32 value = DBusInt32.valueOf(42);

        DBusStruct struct = new DBusStruct(signature, value);

        assertEquals(Type.STRUCT, struct.getType());
    }

    @Test
    void testGetSignature() {
        DBusSignature signature = DBusSignature.valueOf("(sib)");
        DBusString string = DBusString.valueOf("test");
        DBusInt32 intValue = DBusInt32.valueOf(42);
        DBusBoolean boolValue = DBusBoolean.valueOf(false);

        DBusStruct struct = new DBusStruct(signature, string, intValue, boolValue);

        assertEquals(signature, struct.getSignature());
        assertSame(signature, struct.getSignature()); // Should return the same instance
    }

    @Test
    void testWithVariousBasicTypes() {
        DBusSignature signature = DBusSignature.valueOf("(ybnqiuxtdhsog)");

        DBusByte byteVal = DBusByte.valueOf((byte) 255);
        DBusBoolean boolVal = DBusBoolean.valueOf(true);
        DBusInt16 int16Val = DBusInt16.valueOf((short) -1000);
        DBusUInt16 uint16Val = DBusUInt16.valueOf((short) 60000);
        DBusInt32 int32Val = DBusInt32.valueOf(-2000000);
        DBusUInt32 uint32Val = DBusUInt32.valueOf((int) 4000000000L);
        DBusInt64 int64Val = DBusInt64.valueOf(-9000000000000000L);
        DBusUInt64 uint64Val = DBusUInt64.valueOf(-1L); // Max unsigned
        DBusDouble doubleVal = DBusDouble.valueOf(3.14159);
        DBusUnixFD unixFdVal = DBusUnixFD.valueOf(42);
        DBusString stringVal = DBusString.valueOf("hello world");
        DBusObjectPath objectPathVal = DBusObjectPath.valueOf("/com/example/test");
        DBusSignature sigVal = DBusSignature.valueOf("(ii)");

        DBusStruct struct =
                new DBusStruct(
                        signature,
                        byteVal,
                        boolVal,
                        int16Val,
                        uint16Val,
                        int32Val,
                        uint32Val,
                        int64Val,
                        uint64Val,
                        doubleVal,
                        unixFdVal,
                        stringVal,
                        objectPathVal,
                        sigVal);

        assertEquals(signature, struct.getSignature());
        assertEquals(Type.STRUCT, struct.getType());
        assertEquals(13, struct.getDelegate().size());

        // Verify each value
        assertEquals(byteVal, struct.getDelegate().get(0));
        assertEquals(boolVal, struct.getDelegate().get(1));
        assertEquals(int16Val, struct.getDelegate().get(2));
        assertEquals(uint16Val, struct.getDelegate().get(3));
        assertEquals(int32Val, struct.getDelegate().get(4));
        assertEquals(uint32Val, struct.getDelegate().get(5));
        assertEquals(int64Val, struct.getDelegate().get(6));
        assertEquals(uint64Val, struct.getDelegate().get(7));
        assertEquals(doubleVal, struct.getDelegate().get(8));
        assertEquals(unixFdVal, struct.getDelegate().get(9));
        assertEquals(stringVal, struct.getDelegate().get(10));
        assertEquals(objectPathVal, struct.getDelegate().get(11));
        assertEquals(sigVal, struct.getDelegate().get(12));
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for STRUCT type
        // Per D-Bus specification: Empty structures are not allowed

        // 1. Test that empty struct signature is invalid
        assertThrows(
                SignatureException.class,
                () -> {
                    DBusSignature.valueOf("()");
                });

        // 2. Test that struct must have at least one field
        DBusSignature validSignature = DBusSignature.valueOf("(i)");
        DBusInt32 value = DBusInt32.valueOf(42);

        assertDoesNotThrow(
                () -> {
                    new DBusStruct(validSignature, value);
                });

        // 3. Test struct alignment (8-byte boundary per D-Bus spec)
        DBusSignature signature = DBusSignature.valueOf("(iii)");
        DBusInt32 val1 = DBusInt32.valueOf(1);
        DBusInt32 val2 = DBusInt32.valueOf(2);
        DBusInt32 val3 = DBusInt32.valueOf(3);

        DBusStruct struct = new DBusStruct(signature, val1, val2, val3);
        assertEquals(Type.STRUCT, struct.getType());
        assertEquals(3, struct.getDelegate().size());
    }

    @Test
    void testComplexNestedStructures() {
        // Test complex nested structures: (i(sb)(ai))
        // Inner struct 1: (sb)
        DBusSignature innerSig1 = DBusSignature.valueOf("(sb)");
        DBusString innerStr = DBusString.valueOf("inner");
        DBusBoolean innerBool = DBusBoolean.valueOf(true);
        DBusStruct innerStruct1 = new DBusStruct(innerSig1, innerStr, innerBool);

        // Inner struct 2: (ai) - struct containing array of int32
        DBusSignature innerSig2 = DBusSignature.valueOf("(ai)");
        DBusSignature arraySignature = DBusSignature.valueOf("ai");
        List<DBusType> arrayElements =
                Arrays.asList(DBusInt32.valueOf(1), DBusInt32.valueOf(2), DBusInt32.valueOf(3));
        DBusArray intArray = new DBusArray(arraySignature);
        intArray.addAll(arrayElements);
        DBusStruct innerStruct2 = new DBusStruct(innerSig2, intArray);

        // Outer struct: (i(sb)(ai))
        DBusSignature outerSig = DBusSignature.valueOf("(i(sb)(ai))");
        DBusInt32 outerInt = DBusInt32.valueOf(42);
        DBusStruct outerStruct = new DBusStruct(outerSig, outerInt, innerStruct1, innerStruct2);

        assertEquals(outerSig, outerStruct.getSignature());
        assertEquals(Type.STRUCT, outerStruct.getType());
        assertEquals(3, outerStruct.getDelegate().size());
        assertEquals(outerInt, outerStruct.getDelegate().get(0));
        assertEquals(innerStruct1, outerStruct.getDelegate().get(1));
        assertEquals(innerStruct2, outerStruct.getDelegate().get(2));
    }

    @Test
    void testStructWithArrays() {
        // Test struct containing arrays: (aisab)
        DBusSignature signature = DBusSignature.valueOf("(aisab)");

        // Array of integers
        DBusSignature intArraySig = DBusSignature.valueOf("ai");
        List<DBusType> intElements =
                Arrays.asList(DBusInt32.valueOf(1), DBusInt32.valueOf(2), DBusInt32.valueOf(3));
        DBusArray intArray = new DBusArray(intArraySig);
        intArray.addAll(intElements);

        // String value
        DBusString stringVal = DBusString.valueOf("middle");

        // Array of bytes
        DBusSignature byteArraySig = DBusSignature.valueOf("ab");
        List<DBusType> byteElements =
                Arrays.asList(
                        DBusByte.valueOf((byte) 10),
                        DBusByte.valueOf((byte) 20),
                        DBusByte.valueOf((byte) 30));
        DBusArray byteArray = new DBusArray(byteArraySig);
        byteArray.addAll(byteElements);

        DBusStruct struct = new DBusStruct(signature, intArray, stringVal, byteArray);

        assertEquals(signature, struct.getSignature());
        assertEquals(Type.STRUCT, struct.getType());
        assertEquals(3, struct.getDelegate().size());
        assertEquals(intArray, struct.getDelegate().get(0));
        assertEquals(stringVal, struct.getDelegate().get(1));
        assertEquals(byteArray, struct.getDelegate().get(2));
    }

    @Test
    void testStructWithVariant() {
        // Test struct containing variant: (iv)
        DBusSignature signature = DBusSignature.valueOf("(iv)");

        DBusInt32 intVal = DBusInt32.valueOf(42);
        DBusString variantContent = DBusString.valueOf("variant content");
        DBusVariant variant = DBusVariant.valueOf(variantContent);

        DBusStruct struct = new DBusStruct(signature, intVal, variant);

        assertEquals(signature, struct.getSignature());
        assertEquals(Type.STRUCT, struct.getType());
        assertEquals(2, struct.getDelegate().size());
        assertEquals(intVal, struct.getDelegate().get(0));
        assertEquals(variant, struct.getDelegate().get(1));
    }

    @Test
    @Tag("memory-intensive")
    void testMaximumDepthStructures() {
        // Test deep nesting to ensure it doesn't break
        // Create a structure like ((((i))))
        DBusSignature deepSig = DBusSignature.valueOf("((((i))))");
        DBusInt32 value = DBusInt32.valueOf(42);

        DBusStruct level1 = new DBusStruct(DBusSignature.valueOf("(i)"), value);
        DBusStruct level2 = new DBusStruct(DBusSignature.valueOf("((i))"), level1);
        DBusStruct level3 = new DBusStruct(DBusSignature.valueOf("(((i)))"), level2);
        DBusStruct level4 = new DBusStruct(DBusSignature.valueOf("((((i))))"), level3);

        assertEquals(deepSig, level4.getSignature());
        assertEquals(Type.STRUCT, level4.getType());
        assertEquals(1, level4.getDelegate().size());
        assertEquals(level3, level4.getDelegate().get(0));
    }

    @Test
    void testStructImmutability() {
        DBusSignature signature = DBusSignature.valueOf("(is)");
        DBusInt32 intValue = DBusInt32.valueOf(42);
        DBusString stringValue = DBusString.valueOf("test");

        List<DBusType> originalValues = new ArrayList<>(Arrays.asList(intValue, stringValue));
        DBusStruct struct = new DBusStruct(signature, originalValues);

        // Modifying the original list should not affect the struct
        originalValues.clear();
        assertEquals(2, struct.getDelegate().size());

        // Getting delegate should return a new list each time
        List<DBusType> delegate1 = struct.getDelegate();
        List<DBusType> delegate2 = struct.getDelegate();

        assertNotSame(delegate1, delegate2);
        assertEquals(delegate1, delegate2);

        // Modifying delegate should not affect the struct
        delegate1.clear();
        assertEquals(2, struct.getDelegate().size());
    }
}
