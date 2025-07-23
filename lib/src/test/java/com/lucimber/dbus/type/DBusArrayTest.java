/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DBusArrayTest {

    @Test
    void createEmptyArray() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        assertEquals(signature, array.getSignature());
        assertEquals(Type.ARRAY, array.getType());
        assertTrue(array.isEmpty());
        assertEquals(0, array.size());
    }

    @Test
    void createIntegerArray() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);
        DBusInt32 value3 = DBusInt32.valueOf(3);

        array.add(value1);
        array.add(value2);
        array.add(value3);

        assertEquals(3, array.size());
        assertFalse(array.isEmpty());
        assertEquals(value1, array.get(0));
        assertEquals(value2, array.get(1));
        assertEquals(value3, array.get(2));
    }

    @Test
    void createStringArray() {
        DBusSignature signature = DBusSignature.valueOf("as");
        DBusArray<DBusString> array = new DBusArray<>(signature);

        DBusString str1 = DBusString.valueOf("hello");
        DBusString str2 = DBusString.valueOf("world");

        array.add(str1);
        array.add(str2);

        assertEquals(2, array.size());
        assertEquals(str1, array.get(0));
        assertEquals(str2, array.get(1));
    }

    @Test
    void createByteArray() {
        DBusSignature signature = DBusSignature.valueOf("ay");
        DBusArray<DBusByte> array = new DBusArray<>(signature);

        for (int i = 0; i < 256; i++) {
            array.add(DBusByte.valueOf((byte) i));
        }

        assertEquals(256, array.size());
        assertEquals(DBusByte.valueOf((byte) 0), array.get(0));
        assertEquals(DBusByte.valueOf((byte) 255), array.get(255));
    }

    @Test
    void testInvalidSignature() {
        // Non-array signature should throw exception
        DBusSignature invalidSignature = DBusSignature.valueOf("i");

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new DBusArray<>(invalidSignature);
                });
    }

    @Test
    void testNullSignature() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    new DBusArray<DBusInt32>((DBusSignature) null);
                });
    }

    @Test
    void testCopyConstructor() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> original = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);
        original.add(value1);
        original.add(value2);

        DBusArray<DBusInt32> copy = new DBusArray<>(original);

        assertEquals(original.size(), copy.size());
        assertEquals(original.get(0), copy.get(0));
        assertEquals(original.get(1), copy.get(1));
        assertEquals(original.getSignature(), copy.getSignature());

        // Test that they are independent
        DBusInt32 value3 = DBusInt32.valueOf(3);
        copy.add(value3);

        assertEquals(2, original.size());
        assertEquals(3, copy.size());
    }

    @Test
    void testListOperations() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(10);
        DBusInt32 value2 = DBusInt32.valueOf(20);
        DBusInt32 value3 = DBusInt32.valueOf(30);

        // Test add
        assertTrue(array.add(value1));
        assertTrue(array.add(value2));
        array.add(1, value3); // Insert at index 1

        assertEquals(3, array.size());
        assertEquals(value1, array.get(0));
        assertEquals(value3, array.get(1));
        assertEquals(value2, array.get(2));

        // Test set
        DBusInt32 newValue = DBusInt32.valueOf(99);
        DBusInt32 oldValue = array.set(1, newValue);
        assertEquals(value3, oldValue);
        assertEquals(newValue, array.get(1));

        // Test remove
        DBusInt32 removed = array.remove(1);
        assertEquals(newValue, removed);
        assertEquals(2, array.size());

        // Test contains
        assertTrue(array.contains(value1));
        assertTrue(array.contains(value2));
        assertFalse(array.contains(value3));

        // Test indexOf
        assertEquals(0, array.indexOf(value1));
        assertEquals(1, array.indexOf(value2));
        assertEquals(-1, array.indexOf(value3));
    }

    @Test
    void testAddAll() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        List<DBusInt32> values =
                Arrays.asList(DBusInt32.valueOf(1), DBusInt32.valueOf(2), DBusInt32.valueOf(3));

        assertTrue(array.addAll(values));
        assertEquals(3, array.size());
        assertEquals(DBusInt32.valueOf(1), array.get(0));
        assertEquals(DBusInt32.valueOf(2), array.get(1));
        assertEquals(DBusInt32.valueOf(3), array.get(2));

        // Test addAll at index
        List<DBusInt32> moreValues = Arrays.asList(DBusInt32.valueOf(10), DBusInt32.valueOf(20));
        assertTrue(array.addAll(1, moreValues));
        assertEquals(5, array.size());
        assertEquals(DBusInt32.valueOf(1), array.get(0));
        assertEquals(DBusInt32.valueOf(10), array.get(1));
        assertEquals(DBusInt32.valueOf(20), array.get(2));
        assertEquals(DBusInt32.valueOf(2), array.get(3));
        assertEquals(DBusInt32.valueOf(3), array.get(4));
    }

    @Test
    void testRemoveAll() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);
        DBusInt32 value3 = DBusInt32.valueOf(3);

        array.add(value1);
        array.add(value2);
        array.add(value3);
        array.add(value1); // Duplicate

        List<DBusInt32> toRemove = Arrays.asList(value1, value3);
        assertTrue(array.removeAll(toRemove));

        assertEquals(1, array.size());
        assertEquals(value2, array.get(0));
    }

    @Test
    void testRetainAll() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);
        DBusInt32 value3 = DBusInt32.valueOf(3);

        array.add(value1);
        array.add(value2);
        array.add(value3);

        List<DBusInt32> toRetain = Arrays.asList(value1, value3);
        assertTrue(array.retainAll(toRetain));

        assertEquals(2, array.size());
        assertEquals(value1, array.get(0));
        assertEquals(value3, array.get(1));
    }

    @Test
    void testClear() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        array.add(DBusInt32.valueOf(1));
        array.add(DBusInt32.valueOf(2));

        assertEquals(2, array.size());

        array.clear();

        assertEquals(0, array.size());
        assertTrue(array.isEmpty());
    }

    @Test
    void testIterator() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);
        DBusInt32 value3 = DBusInt32.valueOf(3);

        array.add(value1);
        array.add(value2);
        array.add(value3);

        List<DBusInt32> result = new ArrayList<>();
        for (DBusInt32 value : array) {
            result.add(value);
        }

        assertEquals(3, result.size());
        assertEquals(value1, result.get(0));
        assertEquals(value2, result.get(1));
        assertEquals(value3, result.get(2));
    }

    @Test
    void testToArray() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);

        array.add(value1);
        array.add(value2);

        Object[] objArray = array.toArray();
        assertEquals(2, objArray.length);
        assertEquals(value1, objArray[0]);
        assertEquals(value2, objArray[1]);

        DBusInt32[] typedArray = array.toArray(new DBusInt32[0]);
        assertEquals(2, typedArray.length);
        assertEquals(value1, typedArray[0]);
        assertEquals(value2, typedArray[1]);
    }

    @Test
    void testSubList() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        for (int i = 0; i < 10; i++) {
            array.add(DBusInt32.valueOf(i));
        }

        List<DBusInt32> subList = array.subList(2, 5);
        assertEquals(3, subList.size());
        assertEquals(DBusInt32.valueOf(2), subList.get(0));
        assertEquals(DBusInt32.valueOf(3), subList.get(1));
        assertEquals(DBusInt32.valueOf(4), subList.get(2));
    }

    @Test
    void testGetDelegate() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value = DBusInt32.valueOf(42);
        array.add(value);

        List<DBusInt32> delegate = array.getDelegate();
        assertEquals(1, delegate.size());
        assertEquals(value, delegate.get(0));

        // Test that delegate is a copy (immutability)
        delegate.clear();
        assertEquals(1, array.size()); // Original should be unchanged
    }

    @Test
    void testGetDelegateImmutability() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        DBusInt32 value = DBusInt32.valueOf(42);
        array.add(value);

        List<DBusInt32> delegate1 = array.getDelegate();
        List<DBusInt32> delegate2 = array.getDelegate();

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
    void testEquals() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array1 = new DBusArray<>(signature);
        DBusArray<DBusInt32> array2 = new DBusArray<>(signature);
        DBusArray<DBusInt32> array3 = new DBusArray<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);

        array1.add(value1);
        array1.add(value2);

        array2.add(value1);
        array2.add(value2);

        array3.add(value1);
        array3.add(DBusInt32.valueOf(3));

        assertEquals(array1, array2);
        assertEquals(array1, array1); // self-equality
        assertNotEquals(array1, array3);

        assertNotEquals(array1, null);
        assertNotEquals(array1, "string"); // different type
    }

    @Test
    void testEqualsWithDifferentSignatures() {
        DBusSignature intSignature = DBusSignature.valueOf("ai");
        DBusSignature stringSignature = DBusSignature.valueOf("as");

        DBusArray<DBusInt32> intArray = new DBusArray<>(intSignature);
        DBusArray<DBusString> stringArray = new DBusArray<>(stringSignature);

        assertNotEquals(intArray, stringArray);
    }

    @Test
    void testHashCode() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array1 = new DBusArray<>(signature);
        DBusArray<DBusInt32> array2 = new DBusArray<>(signature);
        DBusArray<DBusInt32> array3 = new DBusArray<>(signature);

        DBusInt32 value = DBusInt32.valueOf(42);

        array1.add(value);
        array2.add(value);
        array3.add(DBusInt32.valueOf(99));

        assertEquals(array1.hashCode(), array2.hashCode());
        assertNotEquals(array1.hashCode(), array3.hashCode());
    }

    @Test
    void testGetSignature() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        assertEquals(signature, array.getSignature());
        assertSame(signature, array.getSignature()); // Should return the same instance
    }

    @Test
    void testGetType() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        assertEquals(Type.ARRAY, array.getType());
    }

    @Test
    void testVariousArrayTypes() {
        // Test different array element types

        // Boolean array
        DBusArray<DBusBoolean> boolArray = new DBusArray<>(DBusSignature.valueOf("ab"));
        boolArray.add(DBusBoolean.valueOf(true));
        boolArray.add(DBusBoolean.valueOf(false));
        assertEquals(2, boolArray.size());

        // Byte array
        DBusArray<DBusByte> byteArray = new DBusArray<>(DBusSignature.valueOf("ay"));
        byteArray.add(DBusByte.valueOf((byte) 255));
        assertEquals(1, byteArray.size());

        // Double array
        DBusArray<DBusDouble> doubleArray = new DBusArray<>(DBusSignature.valueOf("ad"));
        doubleArray.add(DBusDouble.valueOf(3.14159));
        assertEquals(1, doubleArray.size());

        // ObjectPath array
        DBusArray<DBusObjectPath> pathArray = new DBusArray<>(DBusSignature.valueOf("ao"));
        pathArray.add(DBusObjectPath.valueOf("/com/example"));
        assertEquals(1, pathArray.size());
    }

    @Test
    void testNestedArrays() {
        // Test array of arrays: aai
        DBusSignature signature = DBusSignature.valueOf("aai");
        DBusArray<DBusArray<DBusInt32>> nestedArray = new DBusArray<>(signature);

        // Create inner arrays
        DBusArray<DBusInt32> innerArray1 = new DBusArray<>(DBusSignature.valueOf("ai"));
        innerArray1.add(DBusInt32.valueOf(1));
        innerArray1.add(DBusInt32.valueOf(2));

        DBusArray<DBusInt32> innerArray2 = new DBusArray<>(DBusSignature.valueOf("ai"));
        innerArray2.add(DBusInt32.valueOf(3));
        innerArray2.add(DBusInt32.valueOf(4));

        nestedArray.add(innerArray1);
        nestedArray.add(innerArray2);

        assertEquals(2, nestedArray.size());
        assertEquals(innerArray1, nestedArray.get(0));
        assertEquals(innerArray2, nestedArray.get(1));
        assertEquals(2, nestedArray.get(0).size());
        assertEquals(DBusInt32.valueOf(1), nestedArray.get(0).get(0));
    }

    @Test
    void testArrayOfStructs() {
        // Test array of structs: a(is)
        DBusSignature signature = DBusSignature.valueOf("a(is)");
        DBusArray<DBusStruct> structArray = new DBusArray<>(signature);

        // Create structs
        DBusSignature structSig = DBusSignature.valueOf("(is)");
        DBusStruct struct1 =
                new DBusStruct(structSig, DBusInt32.valueOf(1), DBusString.valueOf("one"));
        DBusStruct struct2 =
                new DBusStruct(structSig, DBusInt32.valueOf(2), DBusString.valueOf("two"));

        structArray.add(struct1);
        structArray.add(struct2);

        assertEquals(2, structArray.size());
        assertEquals(struct1, structArray.get(0));
        assertEquals(struct2, structArray.get(1));
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for ARRAY type
        // Per D-Bus specification:
        // - Arrays have a maximum size of 2^26 bytes (67108864 bytes)
        // - Arrays must have a single element type
        // - Empty arrays are allowed

        // 1. Test that signature must describe an array
        DBusSignature validSignature = DBusSignature.valueOf("ai");
        assertDoesNotThrow(
                () -> {
                    new DBusArray<>(validSignature);
                });

        // 2. Test that non-array signatures are rejected
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new DBusArray<>(DBusSignature.valueOf("i"));
                });

        // 3. Test that empty arrays are allowed
        DBusArray<DBusInt32> emptyArray = new DBusArray<>(validSignature);
        assertTrue(emptyArray.isEmpty());
        assertEquals(0, emptyArray.size());

        // 4. Test that type is correctly identified
        assertEquals(Type.ARRAY, emptyArray.getType());

        // 5. Test single element type constraint
        DBusArray<DBusInt32> intArray = new DBusArray<>(validSignature);
        intArray.add(DBusInt32.valueOf(42));
        assertEquals(1, intArray.size());
        assertEquals(DBusInt32.valueOf(42), intArray.get(0));
    }

    @Test
    @Tag("memory-intensive")
    void testLargeArray() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        // Add many elements
        for (int i = 0; i < 10000; i++) {
            array.add(DBusInt32.valueOf(i));
        }

        assertEquals(10000, array.size());

        // Test random access
        assertEquals(DBusInt32.valueOf(5000), array.get(5000));
        assertEquals(DBusInt32.valueOf(9999), array.get(9999));

        // Test that all elements are present
        for (int i = 0; i < 10000; i++) {
            assertEquals(DBusInt32.valueOf(i), array.get(i));
        }
    }

    @Test
    void testArrayModificationOperations() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        // Add initial elements
        for (int i = 0; i < 5; i++) {
            array.add(DBusInt32.valueOf(i));
        }

        // Test replaceAll
        array.replaceAll(value -> DBusInt32.valueOf(value.intValue() * 2));

        for (int i = 0; i < 5; i++) {
            assertEquals(DBusInt32.valueOf(i * 2), array.get(i));
        }

        // Test removeIf (remove elements divisible by 4)
        // After replaceAll: [0, 2, 4, 6, 8] -> removeIf %4==0 -> [2, 6]
        array.removeIf(value -> value.intValue() % 4 == 0);

        assertEquals(2, array.size());
        assertEquals(DBusInt32.valueOf(2), array.get(0));
        assertEquals(DBusInt32.valueOf(6), array.get(1));
    }

    @Test
    void testArrayStreaming() {
        DBusSignature signature = DBusSignature.valueOf("ai");
        DBusArray<DBusInt32> array = new DBusArray<>(signature);

        for (int i = 1; i <= 10; i++) {
            array.add(DBusInt32.valueOf(i));
        }

        // Test stream operations
        long evenCount = array.stream().filter(value -> value.intValue() % 2 == 0).count();

        assertEquals(5, evenCount);

        // Test parallel stream
        int sum = array.parallelStream().mapToInt(DBusInt32::intValue).sum();

        assertEquals(55, sum); // 1+2+3+...+10 = 55
    }
}
