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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class DBusDictEntryTest {

  @Test
  void createDictEntryWithKeyOnly() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key);
    
    assertEquals(signature, entry.getSignature());
    assertEquals(Type.DICT_ENTRY, entry.getType());
    assertEquals(key, entry.getKey());
    assertNull(entry.getValue());
  }

  @Test
  void createDictEntryWithKeyAndValue() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, value);
    
    assertEquals(signature, entry.getSignature());
    assertEquals(Type.DICT_ENTRY, entry.getType());
    assertEquals(key, entry.getKey());
    assertEquals(value, entry.getValue());
  }

  @Test
  void createDictEntryWithVariousTypes() {
    // Test with different key-value type combinations
    
    // Boolean-String
    DBusSignature boolStringSig = DBusSignature.valueOf("{bs}");
    DBusBoolean boolKey = DBusBoolean.valueOf(true);
    DBusString stringValue = DBusString.valueOf("true");
    
    DBusDictEntry<DBusBoolean, DBusString> boolStringEntry = 
      new DBusDictEntry<>(boolStringSig, boolKey, stringValue);
    
    assertEquals(boolKey, boolStringEntry.getKey());
    assertEquals(stringValue, boolStringEntry.getValue());
    
    // Byte-Double
    DBusSignature byteDoubleSig = DBusSignature.valueOf("{yd}");
    DBusByte byteKey = DBusByte.valueOf((byte) 255);
    DBusDouble doubleValue = DBusDouble.valueOf(3.14159);
    
    DBusDictEntry<DBusByte, DBusDouble> byteDoubleEntry = 
      new DBusDictEntry<>(byteDoubleSig, byteKey, doubleValue);
    
    assertEquals(byteKey, byteDoubleEntry.getKey());
    assertEquals(doubleValue, byteDoubleEntry.getValue());
  }

  @Test
  void testInvalidSignature() {
    // Non-dict-entry signature should throw exception
    DBusSignature invalidSignature = DBusSignature.valueOf("i");
    DBusString key = DBusString.valueOf("key");
    
    assertThrows(IllegalArgumentException.class, () -> {
      new DBusDictEntry<>(invalidSignature, key);
    });
  }

  @Test
  void testNullSignature() {
    DBusString key = DBusString.valueOf("key");
    
    assertThrows(NullPointerException.class, () -> {
      new DBusDictEntry<>(null, key);
    });
  }

  @Test
  void testNullKey() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    
    assertThrows(NullPointerException.class, () -> {
      new DBusDictEntry<>(signature, null);
    });
  }

  @Test
  void testCopyConstructor() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> original = new DBusDictEntry<>(signature, key, value);
    DBusDictEntry<DBusString, DBusInt32> copy = new DBusDictEntry<>(original);
    
    assertEquals(original.getSignature(), copy.getSignature());
    assertEquals(original.getKey(), copy.getKey());
    assertEquals(original.getValue(), copy.getValue());
    
    // Test that they are independent for value changes
    DBusInt32 newValue = DBusInt32.valueOf(99);
    copy.setValue(newValue);
    
    assertEquals(value, original.getValue());
    assertEquals(newValue, copy.getValue());
  }

  @Test
  void testSetValue() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 oldValue = DBusInt32.valueOf(42);
    DBusInt32 newValue = DBusInt32.valueOf(123);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, oldValue);
    
    assertEquals(oldValue, entry.getValue());
    
    DBusInt32 returned = entry.setValue(newValue);
    
    assertEquals(oldValue, returned);
    assertEquals(newValue, entry.getValue());
  }

  @Test
  void testSetValueFromNull() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key);
    
    assertNull(entry.getValue());
    
    DBusInt32 returned = entry.setValue(value);
    
    assertNull(returned);
    assertEquals(value, entry.getValue());
  }

  @Test
  void testSetValueToNull() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, value);
    
    assertEquals(value, entry.getValue());
    
    DBusInt32 returned = entry.setValue(null);
    
    assertEquals(value, returned);
    assertNull(entry.getValue());
  }

  @Test
  void testEquals() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key1 = DBusString.valueOf("key1");
    DBusString key2 = DBusString.valueOf("key2");
    DBusInt32 value1 = DBusInt32.valueOf(42);
    DBusInt32 value2 = DBusInt32.valueOf(123);
    
    DBusDictEntry<DBusString, DBusInt32> entry1 = new DBusDictEntry<>(signature, key1, value1);
    DBusDictEntry<DBusString, DBusInt32> entry2 = new DBusDictEntry<>(signature, key1, value1);
    DBusDictEntry<DBusString, DBusInt32> entry3 = new DBusDictEntry<>(signature, key2, value1);
    DBusDictEntry<DBusString, DBusInt32> entry4 = new DBusDictEntry<>(signature, key1, value2);
    
    assertEquals(entry1, entry2);
    assertEquals(entry1, entry1); // self-equality
    assertNotEquals(entry1, entry3); // different key
    assertNotEquals(entry1, entry4); // different value
    
    assertNotEquals(entry1, null);
    assertNotEquals(entry1, "string"); // different type
  }

  @Test
  void testEqualsWithNullValues() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    
    DBusDictEntry<DBusString, DBusInt32> entry1 = new DBusDictEntry<>(signature, key, null);
    DBusDictEntry<DBusString, DBusInt32> entry2 = new DBusDictEntry<>(signature, key, null);
    DBusDictEntry<DBusString, DBusInt32> entry3 = new DBusDictEntry<>(signature, key, DBusInt32.valueOf(42));
    
    assertEquals(entry1, entry2);
    assertNotEquals(entry1, entry3);
    assertNotEquals(entry3, entry1);
  }

  @Test
  void testHashCode() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry1 = new DBusDictEntry<>(signature, key, value);
    DBusDictEntry<DBusString, DBusInt32> entry2 = new DBusDictEntry<>(signature, key, value);
    DBusDictEntry<DBusString, DBusInt32> entry3 = new DBusDictEntry<>(signature, key, DBusInt32.valueOf(123));
    
    assertEquals(entry1.hashCode(), entry2.hashCode());
    assertNotEquals(entry1.hashCode(), entry3.hashCode());
  }

  @Test
  void testToString() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, value);
    
    String result = entry.toString();
    assertTrue(result.contains("key"));
    assertTrue(result.contains("42"));
    assertTrue(result.contains("="));
  }

  @Test
  void testToStringWithNullValue() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, null);
    
    String result = entry.toString();
    assertTrue(result.contains("key"));
    assertTrue(result.contains("null"));
    assertTrue(result.contains("="));
  }

  @Test
  void testGetDelegate() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, value);
    
    Map.Entry<DBusString, DBusInt32> delegate = entry.getDelegate();
    assertEquals(key, delegate.getKey());
    assertEquals(value, delegate.getValue());
    
    // Test that delegate is a copy (immutability)
    DBusInt32 newValue = DBusInt32.valueOf(99);
    delegate.setValue(newValue);
    
    assertEquals(value, entry.getValue()); // Original should be unchanged
  }

  @Test
  void testGetDelegateWithNullValue() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, null);
    
    Map.Entry<DBusString, DBusInt32> delegate = entry.getDelegate();
    assertEquals(key, delegate.getKey());
    assertNull(delegate.getValue());
  }

  @Test
  void testGetSignature() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key);
    
    assertEquals(signature, entry.getSignature());
    assertSame(signature, entry.getSignature()); // Should return the same instance
  }

  @Test
  void testGetType() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key);
    
    assertEquals(Type.DICT_ENTRY, entry.getType());
  }

  @Test
  void testWithComplexValueTypes() {
    // Test with array value
    DBusSignature arrayValueSig = DBusSignature.valueOf("{sai}");
    DBusString key = DBusString.valueOf("numbers");
    
    DBusSignature intArraySig = DBusSignature.valueOf("ai");
    DBusArray arrayValue = new DBusArray(intArraySig);
    arrayValue.add(DBusInt32.valueOf(1));
    arrayValue.add(DBusInt32.valueOf(2));
    arrayValue.add(DBusInt32.valueOf(3));
    
    DBusDictEntry<DBusString, DBusArray> arrayEntry = 
      new DBusDictEntry<>(arrayValueSig, key, arrayValue);
    
    assertEquals(key, arrayEntry.getKey());
    assertEquals(arrayValue, arrayEntry.getValue());
    
    // Test with struct value
    DBusSignature structValueSig = DBusSignature.valueOf("{s(ii)}");
    DBusString structKey = DBusString.valueOf("coordinates");
    
    DBusSignature structSig = DBusSignature.valueOf("(ii)");
    DBusStruct structValue = new DBusStruct(structSig, 
      DBusInt32.valueOf(10), DBusInt32.valueOf(20));
    
    DBusDictEntry<DBusString, DBusStruct> structEntry = 
      new DBusDictEntry<>(structValueSig, structKey, structValue);
    
    assertEquals(structKey, structEntry.getKey());
    assertEquals(structValue, structEntry.getValue());
  }

  @Test
  void testWithAllBasicKeyTypes() {
    // Test with different basic key types
    
    // Boolean key
    DBusSignature boolSig = DBusSignature.valueOf("{bi}");
    DBusDictEntry<DBusBoolean, DBusInt32> boolEntry = 
      new DBusDictEntry<>(boolSig, DBusBoolean.valueOf(true), DBusInt32.valueOf(1));
    assertEquals(DBusBoolean.valueOf(true), boolEntry.getKey());
    
    // Byte key
    DBusSignature byteSig = DBusSignature.valueOf("{yi}");
    DBusDictEntry<DBusByte, DBusInt32> byteEntry = 
      new DBusDictEntry<>(byteSig, DBusByte.valueOf((byte) 255), DBusInt32.valueOf(255));
    assertEquals(DBusByte.valueOf((byte) 255), byteEntry.getKey());
    
    // Int16 key
    DBusSignature int16Sig = DBusSignature.valueOf("{ni}");
    DBusDictEntry<DBusInt16, DBusInt32> int16Entry = 
      new DBusDictEntry<>(int16Sig, DBusInt16.valueOf((short) 1000), DBusInt32.valueOf(1000));
    assertEquals(DBusInt16.valueOf((short) 1000), int16Entry.getKey());
    
    // UInt16 key
    DBusSignature uint16Sig = DBusSignature.valueOf("{qi}");
    DBusDictEntry<DBusUInt16, DBusInt32> uint16Entry = 
      new DBusDictEntry<>(uint16Sig, DBusUInt16.valueOf((short) 60000), DBusInt32.valueOf(60000));
    assertEquals(DBusUInt16.valueOf((short) 60000), uint16Entry.getKey());
    
    // Int32 key
    DBusSignature int32Sig = DBusSignature.valueOf("{ii}");
    DBusDictEntry<DBusInt32, DBusInt32> int32Entry = 
      new DBusDictEntry<>(int32Sig, DBusInt32.valueOf(100000), DBusInt32.valueOf(100000));
    assertEquals(DBusInt32.valueOf(100000), int32Entry.getKey());
    
    // UInt32 key
    DBusSignature uint32Sig = DBusSignature.valueOf("{ui}");
    DBusDictEntry<DBusUInt32, DBusInt32> uint32Entry = 
      new DBusDictEntry<>(uint32Sig, DBusUInt32.valueOf((int) 4000000000L), DBusInt32.valueOf(1));
    assertEquals(DBusUInt32.valueOf((int) 4000000000L), uint32Entry.getKey());
    
    // Int64 key
    DBusSignature int64Sig = DBusSignature.valueOf("{xi}");
    DBusDictEntry<DBusInt64, DBusInt32> int64Entry = 
      new DBusDictEntry<>(int64Sig, DBusInt64.valueOf(9000000000000000L), DBusInt32.valueOf(1));
    assertEquals(DBusInt64.valueOf(9000000000000000L), int64Entry.getKey());
    
    // UInt64 key
    DBusSignature uint64Sig = DBusSignature.valueOf("{ti}");
    DBusDictEntry<DBusUInt64, DBusInt32> uint64Entry = 
      new DBusDictEntry<>(uint64Sig, DBusUInt64.valueOf(-1L), DBusInt32.valueOf(1));
    assertEquals(DBusUInt64.valueOf(-1L), uint64Entry.getKey());
    
    // Double key
    DBusSignature doubleSig = DBusSignature.valueOf("{di}");
    DBusDictEntry<DBusDouble, DBusInt32> doubleEntry = 
      new DBusDictEntry<>(doubleSig, DBusDouble.valueOf(3.14159), DBusInt32.valueOf(1));
    assertEquals(DBusDouble.valueOf(3.14159), doubleEntry.getKey());
    
    // String key
    DBusSignature stringSig = DBusSignature.valueOf("{si}");
    DBusDictEntry<DBusString, DBusInt32> stringEntry = 
      new DBusDictEntry<>(stringSig, DBusString.valueOf("hello"), DBusInt32.valueOf(1));
    assertEquals(DBusString.valueOf("hello"), stringEntry.getKey());
    
    // ObjectPath key
    DBusSignature objectPathSig = DBusSignature.valueOf("{oi}");
    DBusDictEntry<DBusObjectPath, DBusInt32> objectPathEntry = 
      new DBusDictEntry<>(objectPathSig, DBusObjectPath.valueOf("/com/example"), DBusInt32.valueOf(1));
    assertEquals(DBusObjectPath.valueOf("/com/example"), objectPathEntry.getKey());
    
    // UnixFD key
    DBusSignature unixFdSig = DBusSignature.valueOf("{hi}");
    DBusDictEntry<DBusUnixFD, DBusInt32> unixFdEntry = 
      new DBusDictEntry<>(unixFdSig, DBusUnixFD.valueOf(42), DBusInt32.valueOf(1));
    assertEquals(DBusUnixFD.valueOf(42), unixFdEntry.getKey());
  }

  @Test
  void testDBusSpecificationCompliance() {
    // Test D-Bus specification compliance for DICT_ENTRY type
    // Per D-Bus specification:
    // - Dict entries have exactly two fields: key and value
    // - Key must be a basic type (not a container)
    // - Value can be any type
    // - Dict entries only occur as array elements
    
    // 1. Test that signature must describe a dict-entry
    DBusSignature validSignature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    assertDoesNotThrow(() -> {
      new DBusDictEntry<>(validSignature, key, value);
    });
    
    // 2. Test that non-dict-entry signatures are rejected
    assertThrows(IllegalArgumentException.class, () -> {
      new DBusDictEntry<>(DBusSignature.valueOf("(si)"), key, value);
    });
    
    // 3. Test that dict-entry type is correctly identified
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(validSignature, key, value);
    assertEquals(Type.DICT_ENTRY, entry.getType());
    
    // 4. Test that key is immutable but value is mutable
    assertEquals(key, entry.getKey());
    assertEquals(value, entry.getValue());
    
    DBusInt32 newValue = DBusInt32.valueOf(99);
    entry.setValue(newValue);
    assertEquals(key, entry.getKey()); // Key unchanged
    assertEquals(newValue, entry.getValue()); // Value changed
  }

  @Test
  void testMapEntryInterface() {
    // Test that DBusDictEntry properly implements Map.Entry interface
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, value);
    
    // Test Map.Entry interface methods
    assertEquals(key, entry.getKey());
    assertEquals(value, entry.getValue());
    
    DBusInt32 newValue = DBusInt32.valueOf(99);
    DBusInt32 oldValue = entry.setValue(newValue);
    
    assertEquals(value, oldValue);
    assertEquals(newValue, entry.getValue());
  }

  @Test
  void testImmutability() {
    DBusSignature signature = DBusSignature.valueOf("{si}");
    DBusString key = DBusString.valueOf("key");
    DBusInt32 value = DBusInt32.valueOf(42);
    
    DBusDictEntry<DBusString, DBusInt32> entry = new DBusDictEntry<>(signature, key, value);
    
    // Test that key is immutable
    DBusString retrievedKey = entry.getKey();
    assertSame(key, retrievedKey); // Should return the same instance
    
    // Test that signature is immutable
    DBusSignature retrievedSignature = entry.getSignature();
    assertSame(signature, retrievedSignature); // Should return the same instance
    
    // Test that changing value doesn't affect the key or signature
    entry.setValue(DBusInt32.valueOf(99));
    assertSame(key, entry.getKey());
    assertSame(signature, entry.getSignature());
  }
}