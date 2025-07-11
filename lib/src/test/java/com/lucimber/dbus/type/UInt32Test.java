/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class UInt32Test {

  @Test
  void convertMinSignedIntegerToInteger() {
    final int expected = Integer.MIN_VALUE;
    final UInt32 uInt32 = UInt32.valueOf(0x80000000);
    final int value = uInt32.intValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMinSignedIntegerToLong() {
    final long expected = 2147483648L;
    final UInt32 uInt32 = UInt32.valueOf(0x80000000);
    final long value = uInt32.longValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMinSignedIntegerToString() {
    final String expected = "2147483648";
    final UInt32 uInt32 = UInt32.valueOf(0x80000000);
    final String value = uInt32.toString();
    assertEquals(expected, value);
  }

  @Test
  void convertMinUnsignedIntegerToInteger() {
    final int expected = 0;
    final UInt32 uInt32 = UInt32.valueOf(0x00000000);
    final int value = uInt32.intValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMinUnsignedIntegerToLong() {
    final long expected = 0L;
    final UInt32 uInt32 = UInt32.valueOf(0x00000000);
    final long value = uInt32.longValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMinUnsignedIntegerToString() {
    final String expected = "0";
    final UInt32 uInt32 = UInt32.valueOf(0x00000000);
    final String value = uInt32.toString();
    assertEquals(expected, value);
  }

  @Test
  void convertMaxSignedIntegerToInteger() {
    final int expected = 2147483647;
    final UInt32 uInt32 = UInt32.valueOf(0x7FFFFFFF);
    final int value = uInt32.intValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMaxSignedIntegerToLong() {
    final long expected = 2147483647L;
    final UInt32 uInt32 = UInt32.valueOf(0x7FFFFFFF);
    final long value = uInt32.longValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMaxSignedIntegerToString() {
    final String expected = "2147483647";
    final UInt32 uInt32 = UInt32.valueOf(0x7FFFFFFF);
    final String value = uInt32.toString();
    assertEquals(expected, value);
  }

  @Test
  void convertMaxUnsignedIntegerToInteger() {
    final int expected = -1;
    final UInt32 uInt32 = UInt32.valueOf(0xFFFFFFFF);
    final int value = uInt32.intValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMaxUnsignedIntegerToLong() {
    final long expected = 4294967295L;
    final UInt32 uInt32 = UInt32.valueOf(0xFFFFFFFF);
    final long value = uInt32.longValue();
    assertEquals(expected, value);
  }

  @Test
  void convertMaxUnsignedIntegerToString() {
    final String expected = "4294967295";
    final UInt32 uInt32 = UInt32.valueOf(0xFFFFFFFF);
    final String value = uInt32.toString();
    assertEquals(expected, value);
  }

  @Test
  void testEquals() {
    UInt32 uint1 = UInt32.valueOf(123);
    UInt32 uint2 = UInt32.valueOf(123);
    UInt32 uint3 = UInt32.valueOf(456);
    
    assertEquals(uint1, uint2);
    assertNotEquals(uint1, uint3);
    assertEquals(uint1, uint1); // self-equality
    
    assertNotEquals(uint1, null);
    assertNotEquals(uint1, "123"); // Different type
    assertNotEquals(uint1, 123); // Different type
  }

  @Test
  void testHashCode() {
    UInt32 uint1 = UInt32.valueOf(123);
    UInt32 uint2 = UInt32.valueOf(123);
    UInt32 uint3 = UInt32.valueOf(456);
    
    assertEquals(uint1.hashCode(), uint2.hashCode());
    assertNotEquals(uint1.hashCode(), uint3.hashCode());
  }

  @Test
  void testCompareTo() {
    UInt32 small = UInt32.valueOf(10);
    UInt32 medium = UInt32.valueOf(20);
    UInt32 large = UInt32.valueOf(30);
    UInt32 duplicate = UInt32.valueOf(20);
    
    assertTrue(small.compareTo(medium) < 0);
    assertTrue(medium.compareTo(large) < 0);
    assertTrue(large.compareTo(medium) > 0);
    assertTrue(medium.compareTo(small) > 0);
    assertEquals(0, medium.compareTo(duplicate));
    assertEquals(0, medium.compareTo(medium));
  }

  @Test
  void testCompareToWithExtremeValues() {
    UInt32 min = UInt32.valueOf(0);
    UInt32 max = UInt32.valueOf(0xFFFFFFFF);
    UInt32 mid = UInt32.valueOf(0x80000000);
    
    assertTrue(min.compareTo(max) < 0);
    assertTrue(max.compareTo(min) > 0);
    assertTrue(min.compareTo(mid) < 0);
    assertTrue(mid.compareTo(min) > 0);
    assertTrue(mid.compareTo(max) < 0);
    assertTrue(max.compareTo(mid) > 0);
  }

  @Test
  void testGetType() {
    assertEquals(Type.UINT32, UInt32.valueOf(0).getType());
    assertEquals(Type.UINT32, UInt32.valueOf(0xFFFFFFFF).getType());
    assertEquals(Type.UINT32, UInt32.valueOf(0x80000000).getType());
  }

  @Test
  void testGetDelegate() {
    int value = 12345;
    UInt32 uint32 = UInt32.valueOf(value);
    
    assertEquals(Integer.valueOf(value), uint32.getDelegate());
    assertEquals(value, uint32.getDelegate().intValue());
  }

  @Test
  void testNumberMethods() {
    UInt32 uint32 = UInt32.valueOf(12345);
    
    assertEquals(12345, uint32.intValue());
    assertEquals(12345L, uint32.longValue());
    assertEquals(12345.0f, uint32.floatValue(), 0.0f);
    assertEquals(12345.0, uint32.doubleValue(), 0.0);
    assertEquals((byte) 12345, uint32.byteValue()); // Truncated
    assertEquals((short) 12345, uint32.shortValue()); // Truncated
  }

  @Test
  void testUnsignedComparisonLogic() {
    // Test that comparison works correctly for unsigned values
    UInt32 large1 = UInt32.valueOf(0x80000000); // > Integer.MAX_VALUE
    UInt32 large2 = UInt32.valueOf(0x90000000); // > Integer.MAX_VALUE
    UInt32 small = UInt32.valueOf(1000);          // < Integer.MAX_VALUE
    
    assertTrue(small.compareTo(large1) < 0);
    assertTrue(large1.compareTo(large2) < 0);
    assertTrue(large2.compareTo(small) > 0);
    assertTrue(large2.compareTo(large1) > 0);
  }

  @Test
  void testCompareToIsConsistentWithEquals() {
    UInt32 uint1 = UInt32.valueOf(123);
    UInt32 uint2 = UInt32.valueOf(123);
    UInt32 uint3 = UInt32.valueOf(456);
    
    // If compareTo returns 0, equals should return true
    assertEquals(0, uint1.compareTo(uint2));
    assertEquals(uint1, uint2);
    
    // If compareTo returns non-zero, equals should return false
    assertNotEquals(0, uint1.compareTo(uint3));
    assertNotEquals(uint1, uint3);
  }

  @Test
  void testSignedIntegerBoundaryBehavior() {
    // Test behavior at the signed integer boundary
    UInt32 maxSigned = UInt32.valueOf(0x7FFFFFFF); // Integer.MAX_VALUE
    UInt32 minUnsigned = UInt32.valueOf(0x80000000); // Where int becomes negative
    
    assertEquals(0x7FFFFFFF, maxSigned.longValue());
    assertEquals(0x7FFFFFFF, maxSigned.intValue());
    
    assertEquals(0x80000000L, minUnsigned.longValue());
    assertEquals(Integer.MIN_VALUE, minUnsigned.intValue()); // Wraps to negative
    
    assertTrue(maxSigned.compareTo(minUnsigned) < 0); // 0x7FFFFFFF < 0x80000000 in unsigned comparison
  }
}
