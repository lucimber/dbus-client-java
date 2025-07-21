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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class DBusBooleanTest {

  @Test
  void createBooleanTrue() {
    DBusBoolean dbusBoolean = DBusBoolean.valueOf(true);
    assertTrue(dbusBoolean.getDelegate());
    assertEquals(Type.BOOLEAN, dbusBoolean.getType());
    assertEquals("true", dbusBoolean.toString());
  }

  @Test
  void createBooleanFalse() {
    DBusBoolean dbusBoolean = DBusBoolean.valueOf(false);
    assertFalse(dbusBoolean.getDelegate());
    assertEquals(Type.BOOLEAN, dbusBoolean.getType());
    assertEquals("false", dbusBoolean.toString());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void createBooleanValues(boolean value) {
    DBusBoolean dbusBoolean = DBusBoolean.valueOf(value);
    assertEquals(value, dbusBoolean.getDelegate());
    assertEquals(Type.BOOLEAN, dbusBoolean.getType());
    assertEquals(Boolean.toString(value), dbusBoolean.toString());
  }

  @Test
  void testEquals() {
    DBusBoolean booleanTrue1 = DBusBoolean.valueOf(true);
    DBusBoolean booleanTrue2 = DBusBoolean.valueOf(true);
    DBusBoolean booleanFalse1 = DBusBoolean.valueOf(false);
    DBusBoolean booleanFalse2 = DBusBoolean.valueOf(false);

    // Test equality
    assertEquals(booleanTrue1, booleanTrue2);
    assertEquals(booleanFalse1, booleanFalse2);
    
    // Test inequality
    assertNotEquals(booleanTrue1, booleanFalse1);
    assertNotEquals(booleanTrue2, booleanFalse2);
    
    // Test self-equality
    assertEquals(booleanTrue1, booleanTrue1);
    assertEquals(booleanFalse1, booleanFalse1);
    
    // Test null inequality
    assertNotEquals(booleanTrue1, null);
    assertNotEquals(booleanFalse1, null);
    
    // Test different type inequality
    assertNotEquals(booleanTrue1, "true");
    assertNotEquals(booleanFalse1, "false");
    assertNotEquals(booleanTrue1, Boolean.TRUE);
    assertNotEquals(booleanFalse1, Boolean.FALSE);
  }

  @Test
  void testHashCode() {
    DBusBoolean booleanTrue1 = DBusBoolean.valueOf(true);
    DBusBoolean booleanTrue2 = DBusBoolean.valueOf(true);
    DBusBoolean booleanFalse1 = DBusBoolean.valueOf(false);
    DBusBoolean booleanFalse2 = DBusBoolean.valueOf(false);

    // Equal objects must have equal hash codes
    assertEquals(booleanTrue1.hashCode(), booleanTrue2.hashCode());
    assertEquals(booleanFalse1.hashCode(), booleanFalse2.hashCode());
    
    // Different objects should have different hash codes (though not guaranteed)
    assertNotEquals(booleanTrue1.hashCode(), booleanFalse1.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("true", DBusBoolean.valueOf(true).toString());
    assertEquals("false", DBusBoolean.valueOf(false).toString());
  }

  @Test
  void testGetType() {
    assertEquals(Type.BOOLEAN, DBusBoolean.valueOf(true).getType());
    assertEquals(Type.BOOLEAN, DBusBoolean.valueOf(false).getType());
  }

  @Test
  void testGetDelegate() {
    assertTrue(DBusBoolean.valueOf(true).getDelegate());
    assertFalse(DBusBoolean.valueOf(false).getDelegate());
    
    // Test that delegate is Boolean type
    assertInstanceOf(Boolean.class, DBusBoolean.valueOf(true).getDelegate());
    assertInstanceOf(Boolean.class, DBusBoolean.valueOf(false).getDelegate());
  }

  @Test
  void testImmutability() {
    // Test that DBusBoolean is immutable
    boolean originalValue = true;
    DBusBoolean dbusBoolean = DBusBoolean.valueOf(originalValue);
    
    // Verify the delegate is as expected
    assertEquals(originalValue, dbusBoolean.getDelegate());
    
    // DBusBoolean should be immutable - no setters to test
    // Just verify that getting the delegate multiple times returns consistent values
    assertEquals(dbusBoolean.getDelegate(), dbusBoolean.getDelegate());
    assertEquals(dbusBoolean.toString(), dbusBoolean.toString());
  }

  @Test
  void testSpecificationCompliance() {
    // Test D-Bus specification compliance for BOOLEAN type
    // Per D-Bus specification: Boolean value: 0 is false, 1 is true, any other value is invalid
    
    // Test that both true and false values are valid
    assertDoesNotThrow(() -> DBusBoolean.valueOf(true));
    assertDoesNotThrow(() -> DBusBoolean.valueOf(false));
    
    // Test that the implementation properly handles boolean values
    DBusBoolean trueBoolean = DBusBoolean.valueOf(true);
    DBusBoolean falseBoolean = DBusBoolean.valueOf(false);
    
    // Verify proper type
    assertEquals(Type.BOOLEAN, trueBoolean.getType());
    assertEquals(Type.BOOLEAN, falseBoolean.getType());
    
    // Verify proper values
    assertTrue(trueBoolean.getDelegate());
    assertFalse(falseBoolean.getDelegate());
  }

  @Test
  void testBooleanPrimitiveIntegration() {
    // Test integration with Java boolean primitives
    boolean primitiveFalse = false;
    boolean primitiveTrue = true;
    
    DBusBoolean dbusFalse = DBusBoolean.valueOf(primitiveFalse);
    DBusBoolean dbusTrue = DBusBoolean.valueOf(primitiveTrue);
    
    assertEquals(primitiveFalse, dbusFalse.getDelegate());
    assertEquals(primitiveTrue, dbusTrue.getDelegate());
  }

  @Test
  void testBooleanObjectIntegration() {
    // Test integration with Java Boolean objects
    Boolean objectFalse = Boolean.FALSE;
    Boolean objectTrue = Boolean.TRUE;
    
    DBusBoolean dbusFalse = DBusBoolean.valueOf(objectFalse);
    DBusBoolean dbusTrue = DBusBoolean.valueOf(objectTrue);
    
    assertEquals(objectFalse, dbusFalse.getDelegate());
    assertEquals(objectTrue, dbusTrue.getDelegate());
  }

  @Test
  void testValidDBusType() {
    // Test that DBusBoolean implements DBusBasicType
    DBusBoolean dbusBoolean = DBusBoolean.valueOf(true);
    
    assertInstanceOf(DBusBasicType.class, dbusBoolean);
    assertEquals(Type.BOOLEAN, dbusBoolean.getType());
  }

  @Test
  void testMultipleInstances() {
    // Test creating multiple instances
    DBusBoolean bool1 = DBusBoolean.valueOf(true);
    DBusBoolean bool2 = DBusBoolean.valueOf(true);
    DBusBoolean bool3 = DBusBoolean.valueOf(false);
    DBusBoolean bool4 = DBusBoolean.valueOf(false);
    
    // All should be equal to their respective values
    assertEquals(bool1, bool2);
    assertEquals(bool3, bool4);
    assertNotEquals(bool1, bool3);
    assertNotEquals(bool2, bool4);
  }

  @Test
  void testConsistentBehavior() {
    // Test that multiple calls with same value produce consistent results
    for (int i = 0; i < 10; i++) {
      DBusBoolean trueBoolean = DBusBoolean.valueOf(true);
      DBusBoolean falseBoolean = DBusBoolean.valueOf(false);
      
      assertTrue(trueBoolean.getDelegate());
      assertFalse(falseBoolean.getDelegate());
      assertEquals("true", trueBoolean.toString());
      assertEquals("false", falseBoolean.toString());
      assertEquals(Type.BOOLEAN, trueBoolean.getType());
      assertEquals(Type.BOOLEAN, falseBoolean.getType());
    }
  }
}