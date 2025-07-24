/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

final class DBusDictTest {

    @Test
    void createEmptyDict() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        assertEquals(signature, dict.getSignature());
        assertEquals(Type.ARRAY, dict.getType()); // Dict is implemented as array of dict-entries
        assertTrue(dict.isEmpty());
        assertEquals(0, dict.size());
    }

    @Test
    void createDictWithStringIntMapping() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key1 = DBusString.valueOf("key1");
        DBusInt32 value1 = DBusInt32.valueOf(42);
        DBusString key2 = DBusString.valueOf("key2");
        DBusInt32 value2 = DBusInt32.valueOf(123);

        dict.put(key1, value1);
        dict.put(key2, value2);

        assertEquals(2, dict.size());
        assertFalse(dict.isEmpty());
        assertEquals(value1, dict.get(key1));
        assertEquals(value2, dict.get(key2));
    }

    @Test
    void createDictWithIntStringMapping() {
        DBusSignature signature = DBusSignature.valueOf("a{is}");
        DBusDict<DBusInt32, DBusString> dict = new DBusDict<>(signature);

        DBusInt32 key1 = DBusInt32.valueOf(1);
        DBusString value1 = DBusString.valueOf("one");
        DBusInt32 key2 = DBusInt32.valueOf(2);
        DBusString value2 = DBusString.valueOf("two");

        dict.put(key1, value1);
        dict.put(key2, value2);

        assertEquals(2, dict.size());
        assertEquals(value1, dict.get(key1));
        assertEquals(value2, dict.get(key2));
    }

    @Test
    void createDictWithByteDoubleMapping() {
        DBusSignature signature = DBusSignature.valueOf("a{yd}");
        DBusDict<DBusByte, DBusDouble> dict = new DBusDict<>(signature);

        DBusByte key1 = DBusByte.valueOf((byte) 10);
        DBusDouble value1 = DBusDouble.valueOf(3.14);
        DBusByte key2 = DBusByte.valueOf((byte) 20);
        DBusDouble value2 = DBusDouble.valueOf(2.71);

        dict.put(key1, value1);
        dict.put(key2, value2);

        assertEquals(2, dict.size());
        assertEquals(value1, dict.get(key1));
        assertEquals(value2, dict.get(key2));
    }

    @Test
    void testInvalidSignature() {
        // Non-dictionary signature should throw exception
        DBusSignature invalidSignature = DBusSignature.valueOf("i");

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new DBusDict<>(invalidSignature);
                });
    }

    @Test
    void testInvalidKeyType() {
        // Container type as key should throw exception (keys must be basic types)
        // This signature is invalid and should fail at signature parsing
        assertThrows(
                SignatureException.class,
                () -> {
                    DBusSignature.valueOf("a{(i)i}"); // struct as key
                });
    }

    @Test
    void testInvalidValueType() {
        // Dict-entry as value should throw exception
        // This signature is invalid and should fail at signature parsing
        assertThrows(
                SignatureException.class,
                () -> {
                    DBusSignature.valueOf("a{s{si}}"); // nested dict-entry
                });
    }

    @Test
    void testNullSignature() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    new DBusDict<DBusString, DBusInt32>((DBusSignature) null);
                });
    }

    @Test
    void testCopyConstructor() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> original = new DBusDict<>(signature);

        DBusString key = DBusString.valueOf("key");
        DBusInt32 value = DBusInt32.valueOf(42);
        original.put(key, value);

        DBusDict<DBusString, DBusInt32> copy = new DBusDict<>(original);

        assertEquals(original.size(), copy.size());
        assertEquals(original.get(key), copy.get(key));
        assertEquals(original.getSignature(), copy.getSignature());

        // Test that they are independent
        DBusString newKey = DBusString.valueOf("newkey");
        DBusInt32 newValue = DBusInt32.valueOf(99);
        copy.put(newKey, newValue);

        assertFalse(original.containsKey(newKey));
        assertTrue(copy.containsKey(newKey));
    }

    @Test
    void testMapOperations() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key1 = DBusString.valueOf("key1");
        DBusInt32 value1 = DBusInt32.valueOf(42);
        DBusString key2 = DBusString.valueOf("key2");
        DBusInt32 value2 = DBusInt32.valueOf(123);

        // Test put and get
        assertNull(dict.put(key1, value1));
        assertEquals(value1, dict.get(key1));

        // Test replace
        assertEquals(value1, dict.put(key1, value2));
        assertEquals(value2, dict.get(key1));

        // Test containsKey and containsValue
        assertTrue(dict.containsKey(key1));
        assertFalse(dict.containsKey(key2));
        assertTrue(dict.containsValue(value2));
        assertFalse(dict.containsValue(value1));

        // Test remove
        assertEquals(value2, dict.remove(key1));
        assertNull(dict.get(key1));
        assertFalse(dict.containsKey(key1));
    }

    @Test
    void testPutAll() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        Map<DBusString, DBusInt32> dataMap = new HashMap<>();
        dataMap.put(DBusString.valueOf("key1"), DBusInt32.valueOf(1));
        dataMap.put(DBusString.valueOf("key2"), DBusInt32.valueOf(2));
        dataMap.put(DBusString.valueOf("key3"), DBusInt32.valueOf(3));

        dict.putAll(dataMap);

        assertEquals(3, dict.size());
        assertEquals(DBusInt32.valueOf(1), dict.get(DBusString.valueOf("key1")));
        assertEquals(DBusInt32.valueOf(2), dict.get(DBusString.valueOf("key2")));
        assertEquals(DBusInt32.valueOf(3), dict.get(DBusString.valueOf("key3")));
    }

    @Test
    void testClear() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        dict.put(DBusString.valueOf("key1"), DBusInt32.valueOf(1));
        dict.put(DBusString.valueOf("key2"), DBusInt32.valueOf(2));

        assertEquals(2, dict.size());

        dict.clear();

        assertEquals(0, dict.size());
        assertTrue(dict.isEmpty());
    }

    @Test
    void testKeySet() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key1 = DBusString.valueOf("key1");
        DBusString key2 = DBusString.valueOf("key2");
        dict.put(key1, DBusInt32.valueOf(1));
        dict.put(key2, DBusInt32.valueOf(2));

        Set<DBusString> keys = dict.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(key1));
        assertTrue(keys.contains(key2));
    }

    @Test
    void testValues() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);
        dict.put(DBusString.valueOf("key1"), value1);
        dict.put(DBusString.valueOf("key2"), value2);

        var values = dict.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(value1));
        assertTrue(values.contains(value2));
    }

    @Test
    void testEntrySet() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key1 = DBusString.valueOf("key1");
        DBusInt32 value1 = DBusInt32.valueOf(1);
        dict.put(key1, value1);

        Set<Map.Entry<DBusString, DBusInt32>> entries = dict.entrySet();
        assertEquals(1, entries.size());

        Map.Entry<DBusString, DBusInt32> entry = entries.iterator().next();
        assertEquals(key1, entry.getKey());
        assertEquals(value1, entry.getValue());
    }

    @Test
    void testDictionaryEntrySet() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key1 = DBusString.valueOf("key1");
        DBusInt32 value1 = DBusInt32.valueOf(1);
        dict.put(key1, value1);

        Set<DBusDictEntry<DBusString, DBusInt32>> dictEntries = dict.dictionaryEntrySet();
        assertEquals(1, dictEntries.size());

        DBusDictEntry<DBusString, DBusInt32> dictEntry = dictEntries.iterator().next();
        assertEquals(key1, dictEntry.getKey());
        assertEquals(value1, dictEntry.getValue());
    }

    @Test
    void testGetDelegate() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key = DBusString.valueOf("key");
        DBusInt32 value = DBusInt32.valueOf(42);
        dict.put(key, value);

        Map<DBusString, DBusInt32> delegate = dict.getDelegate();
        assertEquals(1, delegate.size());
        assertEquals(value, delegate.get(key));

        // Test that delegate is a copy (immutability)
        delegate.clear();
        assertEquals(1, dict.size()); // Original should be unchanged
    }

    @Test
    void testGetDelegateImmutability() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key = DBusString.valueOf("key");
        DBusInt32 value = DBusInt32.valueOf(42);
        dict.put(key, value);

        Map<DBusString, DBusInt32> delegate1 = dict.getDelegate();
        Map<DBusString, DBusInt32> delegate2 = dict.getDelegate();

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
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        DBusString key = DBusString.valueOf("key");
        DBusInt32 value = DBusInt32.valueOf(42);
        dict.put(key, value);

        String result = dict.toString();
        assertNotNull(result);
        assertTrue(result.contains("key"));
        assertTrue(result.contains("42"));
    }

    @Test
    void testGetSignature() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        assertEquals(signature, dict.getSignature());
        assertSame(signature, dict.getSignature()); // Should return the same instance
    }

    @Test
    void testGetType() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        assertEquals(Type.ARRAY, dict.getType()); // Dict is implemented as array of dict-entries
    }

    @Test
    void testVariousKeyTypes() {
        // Test with different basic key types

        // Boolean key
        DBusSignature boolSig = DBusSignature.valueOf("a{bi}");
        DBusDict<DBusBoolean, DBusInt32> boolDict = new DBusDict<>(boolSig);
        boolDict.put(DBusBoolean.valueOf(true), DBusInt32.valueOf(1));
        boolDict.put(DBusBoolean.valueOf(false), DBusInt32.valueOf(0));
        assertEquals(2, boolDict.size());

        // Byte key
        DBusSignature byteSig = DBusSignature.valueOf("a{yi}");
        DBusDict<DBusByte, DBusInt32> byteDict = new DBusDict<>(byteSig);
        byteDict.put(DBusByte.valueOf((byte) 255), DBusInt32.valueOf(255));
        assertEquals(1, byteDict.size());

        // ObjectPath key
        DBusSignature objectPathSig = DBusSignature.valueOf("a{oi}");
        DBusDict<DBusObjectPath, DBusInt32> objectPathDict = new DBusDict<>(objectPathSig);
        objectPathDict.put(DBusObjectPath.valueOf("/com/example"), DBusInt32.valueOf(1));
        assertEquals(1, objectPathDict.size());
    }

    @Test
    void testVariousValueTypes() {
        // Test with different value types

        // Array value
        DBusSignature arrayValueSig = DBusSignature.valueOf("a{sai}");
        DBusDict<DBusString, DBusArray> arrayDict = new DBusDict<>(arrayValueSig);

        DBusSignature intArraySig = DBusSignature.valueOf("ai");
        DBusArray intArray = new DBusArray(intArraySig);
        intArray.add(DBusInt32.valueOf(1));
        intArray.add(DBusInt32.valueOf(2));
        intArray.add(DBusInt32.valueOf(3));

        arrayDict.put(DBusString.valueOf("numbers"), intArray);
        assertEquals(1, arrayDict.size());

        // Struct value
        DBusSignature structValueSig = DBusSignature.valueOf("a{s(ii)}");
        DBusDict<DBusString, DBusStruct> structDict = new DBusDict<>(structValueSig);

        DBusSignature structSig = DBusSignature.valueOf("(ii)");
        DBusStruct struct = new DBusStruct(structSig, DBusInt32.valueOf(1), DBusInt32.valueOf(2));

        structDict.put(DBusString.valueOf("coordinates"), struct);
        assertEquals(1, structDict.size());
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for dictionary type
        // Per D-Bus specification:
        // - Dictionary is an array of dictionary entries
        // - Keys must be basic types (no containers)
        // - Values can be any type except dictionary entries

        // 1. Test that dictionary signature is correctly handled
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        assertEquals(signature, dict.getSignature());
        assertEquals(Type.ARRAY, dict.getType()); // Dictionary is array of dict-entries

        // 2. Test key must be basic type - invalid signatures should fail at parsing
        assertThrows(
                SignatureException.class,
                () -> {
                    DBusSignature.valueOf("a{(ii)s}"); // struct as key
                });

        // 3. Test value cannot be dict-entry - invalid signatures should fail at parsing
        assertThrows(
                SignatureException.class,
                () -> {
                    DBusSignature.valueOf("a{s{si}}"); // nested dict-entry
                });

        // 4. Test duplicate keys are handled (last one wins)
        DBusString key = DBusString.valueOf("duplicate");
        DBusInt32 value1 = DBusInt32.valueOf(1);
        DBusInt32 value2 = DBusInt32.valueOf(2);

        dict.put(key, value1);
        dict.put(key, value2);

        assertEquals(1, dict.size());
        assertEquals(value2, dict.get(key));
    }

    @Test
    void testComplexDictionary() {
        // Test complex dictionary: a{s(ias)}
        DBusSignature signature = DBusSignature.valueOf("a{s(ias)}");
        DBusDict<DBusString, DBusStruct> dict = new DBusDict<>(signature);

        // Create struct value: (ias) - int and array of strings
        DBusSignature structSig = DBusSignature.valueOf("(ias)");
        DBusInt32 intValue = DBusInt32.valueOf(42);

        DBusSignature stringArraySig = DBusSignature.valueOf("as");
        DBusArray stringArray = new DBusArray(stringArraySig);
        stringArray.add(DBusString.valueOf("hello"));
        stringArray.add(DBusString.valueOf("world"));

        DBusStruct structValue = new DBusStruct(structSig, intValue, stringArray);

        dict.put(DBusString.valueOf("data"), structValue);

        assertEquals(1, dict.size());
        assertEquals(structValue, dict.get(DBusString.valueOf("data")));
    }

    @Test
    @Tag("memory-intensive")
    void testLargeDict() {
        DBusSignature signature = DBusSignature.valueOf("a{si}");
        DBusDict<DBusString, DBusInt32> dict = new DBusDict<>(signature);

        // Add many entries
        for (int i = 0; i < 1000; i++) {
            dict.put(DBusString.valueOf("key" + i), DBusInt32.valueOf(i));
        }

        assertEquals(1000, dict.size());

        // Test random access
        assertEquals(DBusInt32.valueOf(500), dict.get(DBusString.valueOf("key500")));
        assertEquals(DBusInt32.valueOf(999), dict.get(DBusString.valueOf("key999")));

        // Test that all keys are present
        for (int i = 0; i < 1000; i++) {
            assertTrue(dict.containsKey(DBusString.valueOf("key" + i)));
        }
    }
}
