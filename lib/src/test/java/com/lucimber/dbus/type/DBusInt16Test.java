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

final class DBusInt16Test {

  @Test
  void createWithMinValue() {
    short min = Short.MIN_VALUE; // -32768
    DBusInt16 int16 = DBusInt16.valueOf(min);
    
    assertEquals(min, int16.shortValue());
    assertEquals(min, int16.getDelegate());
    assertEquals(Type.INT16, int16.getType());
  }

  @Test
  void createWithMaxValue() {
    short max = Short.MAX_VALUE; // 32767
    DBusInt16 int16 = DBusInt16.valueOf(max);
    
    assertEquals(max, int16.shortValue());
    assertEquals(max, int16.getDelegate());
    assertEquals(Type.INT16, int16.getType());
  }

  @Test
  void createWithZero() {
    short zero = 0;
    DBusInt16 int16 = DBusInt16.valueOf(zero);
    
    assertEquals(zero, int16.shortValue());
    assertEquals(zero, int16.getDelegate());
    assertEquals(Type.INT16, int16.getType());
  }

  @Test
  void createWithPositiveValue() {
    short positive = 12345;
    DBusInt16 int16 = DBusInt16.valueOf(positive);
    
    assertEquals(positive, int16.shortValue());
    assertEquals(positive, int16.getDelegate());
    assertEquals(Type.INT16, int16.getType());
  }

  @Test
  void createWithNegativeValue() {
    short negative = -12345;
    DBusInt16 int16 = DBusInt16.valueOf(negative);
    
    assertEquals(negative, int16.shortValue());
    assertEquals(negative, int16.getDelegate());
    assertEquals(Type.INT16, int16.getType());
  }

  @Test
  void testEquals() {
    DBusInt16 int1 = DBusInt16.valueOf((short) 123);
    DBusInt16 int2 = DBusInt16.valueOf((short) 123);
    DBusInt16 int3 = DBusInt16.valueOf((short) 456);
    
    assertEquals(int1, int2);
    assertNotEquals(int1, int3);
    assertEquals(int1, int1); // self-equality
    
    assertNotEquals(int1, null);
    assertNotEquals(int1, "123"); // Different type
    assertNotEquals(int1, 123); // Different type
  }

  @Test
  void testHashCode() {
    DBusInt16 int1 = DBusInt16.valueOf((short) 123);
    DBusInt16 int2 = DBusInt16.valueOf((short) 123);
    DBusInt16 int3 = DBusInt16.valueOf((short) 456);
    
    assertEquals(int1.hashCode(), int2.hashCode());
    assertNotEquals(int1.hashCode(), int3.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("123", DBusInt16.valueOf((short) 123).toString());
    assertEquals("-123", DBusInt16.valueOf((short) -123).toString());
    assertEquals("0", DBusInt16.valueOf((short) 0).toString());
    assertEquals("32767", DBusInt16.valueOf(Short.MAX_VALUE).toString());
    assertEquals("-32768", DBusInt16.valueOf(Short.MIN_VALUE).toString());
  }

  @Test
  void testCompareTo() {
    DBusInt16 small = DBusInt16.valueOf((short) 10);
    DBusInt16 medium = DBusInt16.valueOf((short) 20);
    DBusInt16 large = DBusInt16.valueOf((short) 30);
    DBusInt16 duplicate = DBusInt16.valueOf((short) 20);
    
    assertTrue(small.compareTo(medium) < 0);
    assertTrue(medium.compareTo(large) < 0);
    assertTrue(large.compareTo(medium) > 0);
    assertTrue(medium.compareTo(small) > 0);
    assertEquals(0, medium.compareTo(duplicate));
    assertEquals(0, medium.compareTo(medium));
  }

  @Test
  void testCompareToWithExtremeValues() {
    DBusInt16 min = DBusInt16.valueOf(Short.MIN_VALUE);
    DBusInt16 max = DBusInt16.valueOf(Short.MAX_VALUE);
    DBusInt16 zero = DBusInt16.valueOf((short) 0);
    
    assertTrue(min.compareTo(max) < 0);
    assertTrue(max.compareTo(min) > 0);
    assertTrue(min.compareTo(zero) < 0);
    assertTrue(zero.compareTo(min) > 0);
    assertTrue(zero.compareTo(max) < 0);
    assertTrue(max.compareTo(zero) > 0);
  }

  @Test
  void testNumberMethods() {
    DBusInt16 int16 = DBusInt16.valueOf((short) 12345);
    
    assertEquals(12345, int16.intValue());
    assertEquals(12345L, int16.longValue());
    assertEquals(12345.0f, int16.floatValue(), 0.0f);
    assertEquals(12345.0, int16.doubleValue(), 0.0);
    assertEquals((byte) 12345, int16.byteValue()); // Truncated
    assertEquals((short) 12345, int16.shortValue());
  }

  @Test
  void testNumberMethodsWithNegativeValues() {
    DBusInt16 int16 = DBusInt16.valueOf((short) -12345);
    
    assertEquals(-12345, int16.intValue());
    assertEquals(-12345L, int16.longValue());
    assertEquals(-12345.0f, int16.floatValue(), 0.0f);
    assertEquals(-12345.0, int16.doubleValue(), 0.0);
    assertEquals((byte) -12345, int16.byteValue()); // Truncated
    assertEquals((short) -12345, int16.shortValue());
  }

  @Test
  void testNumberMethodsWithExtremeValues() {
    // Test with MIN_VALUE
    DBusInt16 min = DBusInt16.valueOf(Short.MIN_VALUE);
    assertEquals(Short.MIN_VALUE, min.intValue());
    assertEquals((long) Short.MIN_VALUE, min.longValue());
    assertEquals((float) Short.MIN_VALUE, min.floatValue(), 0.0f);
    assertEquals((double) Short.MIN_VALUE, min.doubleValue(), 0.0);
    
    // Test with MAX_VALUE
    DBusInt16 max = DBusInt16.valueOf(Short.MAX_VALUE);
    assertEquals(Short.MAX_VALUE, max.intValue());
    assertEquals((long) Short.MAX_VALUE, max.longValue());
    assertEquals((float) Short.MAX_VALUE, max.floatValue(), 0.0f);
    assertEquals((double) Short.MAX_VALUE, max.doubleValue(), 0.0);
  }

  @Test
  void testGetDelegate() {
    short value = 12345;
    DBusInt16 int16 = DBusInt16.valueOf(value);
    
    assertEquals(Short.valueOf(value), int16.getDelegate());
    assertEquals(value, int16.getDelegate().shortValue());
  }

  @Test
  void testGetType() {
    assertEquals(Type.INT16, DBusInt16.valueOf((short) 0).getType());
    assertEquals(Type.INT16, DBusInt16.valueOf(Short.MIN_VALUE).getType());
    assertEquals(Type.INT16, DBusInt16.valueOf(Short.MAX_VALUE).getType());
  }

  @Test
  void testBoundaryValues() {
    // Test values at the boundary
    DBusInt16 minValue = DBusInt16.valueOf(Short.MIN_VALUE);
    DBusInt16 maxValue = DBusInt16.valueOf(Short.MAX_VALUE);
    DBusInt16 minPlusOne = DBusInt16.valueOf((short) (Short.MIN_VALUE + 1));
    DBusInt16 maxMinusOne = DBusInt16.valueOf((short) (Short.MAX_VALUE - 1));
    
    assertEquals(Short.MIN_VALUE, minValue.shortValue());
    assertEquals(Short.MAX_VALUE, maxValue.shortValue());
    assertEquals(Short.MIN_VALUE + 1, minPlusOne.shortValue());
    assertEquals(Short.MAX_VALUE - 1, maxMinusOne.shortValue());
    
    assertTrue(minValue.compareTo(minPlusOne) < 0);
    assertTrue(maxMinusOne.compareTo(maxValue) < 0);
  }

  @Test
  void testImmutability() {
    short original = 12345;
    DBusInt16 int16 = DBusInt16.valueOf(original);
    
    // Verify the delegate is the expected value
    assertEquals(original, int16.getDelegate().shortValue());
    
    // Int16 should be immutable - no setters to test
    // Just verify that getting the delegate multiple times returns consistent values
    assertEquals(int16.getDelegate(), int16.getDelegate());
    assertEquals(int16.shortValue(), int16.shortValue());
  }

  @Test
  void testCompareToIsConsistentWithEquals() {
    DBusInt16 int1 = DBusInt16.valueOf((short) 123);
    DBusInt16 int2 = DBusInt16.valueOf((short) 123);
    DBusInt16 int3 = DBusInt16.valueOf((short) 456);
    
    // If compareTo returns 0, equals should return true
    assertEquals(0, int1.compareTo(int2));
    assertEquals(int1, int2);
    
    // If compareTo returns non-zero, equals should return false
    assertNotEquals(0, int1.compareTo(int3));
    assertNotEquals(int1, int3);
  }
}