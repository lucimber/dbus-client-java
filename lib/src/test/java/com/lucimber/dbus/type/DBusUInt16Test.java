/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class DBusUInt16Test {

    @Test
    void createWithMinValue() {
        short min = 0;
        DBusUInt16 uint16 = DBusUInt16.valueOf(min);

        assertEquals(min, uint16.intValue());
        assertEquals(Type.UINT16, uint16.getType());
    }

    @Test
    void createWithMaxValue() {
        short max = (short) 65535; // 2^16 - 1, will be -1 when cast to signed short
        DBusUInt16 uint16 = DBusUInt16.valueOf(max);

        assertEquals(65535, uint16.intValue()); // Should return unsigned value
        assertEquals(Type.UINT16, uint16.getType());
    }

    @Test
    void createWithMidValue() {
        short mid = (short) 32768; // 2^15, will be -32768 when cast to signed short
        DBusUInt16 uint16 = DBusUInt16.valueOf(mid);

        assertEquals(32768, uint16.intValue()); // Should return unsigned value
        assertEquals(Type.UINT16, uint16.getType());
    }

    @Test
    void createWithTypicalValue() {
        short typical = 12345;
        DBusUInt16 uint16 = DBusUInt16.valueOf(typical);

        assertEquals(typical, uint16.intValue());
        assertEquals(Type.UINT16, uint16.getType());
    }

    @Test
    void testSignedToUnsignedConversion() {
        // Test conversion from signed short to unsigned representation
        DBusUInt16 uint16 =
                DBusUInt16.valueOf((short) 0x8000); // 32768 in unsigned, -32768 in signed

        assertEquals(32768, uint16.intValue());
        assertEquals(-32768, uint16.shortValue()); // When cast to signed short
        assertEquals(32768L, uint16.longValue());
        assertEquals("32768", uint16.toString());
    }

    @Test
    void testMaxUnsignedValue() {
        DBusUInt16 uint16 = DBusUInt16.valueOf((short) 0xFFFF); // 65535 in unsigned, -1 in signed

        assertEquals(65535, uint16.intValue());
        assertEquals(-1, uint16.shortValue()); // When cast to signed short
        assertEquals(65535L, uint16.longValue());
        // Note: toString() behavior depends on implementation - may return string representation of
        // signed value
        assertNotNull(uint16.toString());
    }

    @Test
    void testEquals() {
        DBusUInt16 uint1 = DBusUInt16.valueOf((short) 123);
        DBusUInt16 uint2 = DBusUInt16.valueOf((short) 123);
        DBusUInt16 uint3 = DBusUInt16.valueOf((short) 456);

        assertEquals(uint1, uint2);
        assertNotEquals(uint1, uint3);
        assertEquals(uint1, uint1); // self-equality

        assertNotEquals(uint1, null);
        assertNotEquals(uint1, "123"); // Different type
        assertNotEquals(uint1, 123); // Different type
    }

    @Test
    void testHashCode() {
        DBusUInt16 uint1 = DBusUInt16.valueOf((short) 123);
        DBusUInt16 uint2 = DBusUInt16.valueOf((short) 123);
        DBusUInt16 uint3 = DBusUInt16.valueOf((short) 456);

        assertEquals(uint1.hashCode(), uint2.hashCode());
        assertNotEquals(uint1.hashCode(), uint3.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("123", DBusUInt16.valueOf((short) 123).toString());
        assertEquals("0", DBusUInt16.valueOf((short) 0).toString());
        assertEquals("65535", DBusUInt16.valueOf((short) 65535).toString());
        assertEquals("32768", DBusUInt16.valueOf((short) 32768).toString());
    }

    @Test
    void testCompareTo() {
        DBusUInt16 small = DBusUInt16.valueOf((short) 10);
        DBusUInt16 medium = DBusUInt16.valueOf((short) 20);
        DBusUInt16 large = DBusUInt16.valueOf((short) 30);
        DBusUInt16 duplicate = DBusUInt16.valueOf((short) 20);

        assertTrue(small.compareTo(medium) < 0);
        assertTrue(medium.compareTo(large) < 0);
        assertTrue(large.compareTo(medium) > 0);
        assertTrue(medium.compareTo(small) > 0);
        assertEquals(0, medium.compareTo(duplicate));
        assertEquals(0, medium.compareTo(medium));
    }

    @Test
    void testCompareToWithExtremeValues() {
        DBusUInt16 min = DBusUInt16.valueOf((short) 0);
        DBusUInt16 max = DBusUInt16.valueOf((short) 65535);
        DBusUInt16 mid = DBusUInt16.valueOf((short) 32768);

        assertTrue(min.compareTo(max) < 0);
        assertTrue(max.compareTo(min) > 0);
        assertTrue(min.compareTo(mid) < 0);
        assertTrue(mid.compareTo(min) > 0);
        assertTrue(mid.compareTo(max) < 0);
        assertTrue(max.compareTo(mid) > 0);
    }

    @Test
    void testNumberMethods() {
        DBusUInt16 uint16 = DBusUInt16.valueOf((short) 12345);

        assertEquals(12345, uint16.intValue());
        assertEquals(12345L, uint16.longValue());
        assertEquals(12345.0f, uint16.floatValue(), 0.0f);
        assertEquals(12345.0, uint16.doubleValue(), 0.0);
        assertEquals((byte) 12345, uint16.byteValue()); // Truncated
        assertEquals((short) 12345, uint16.shortValue());
    }

    @Test
    void testNumberMethodsWithLargeValues() {
        // Test with value that's positive in unsigned but negative in signed
        DBusUInt16 uint16 = DBusUInt16.valueOf((short) 40000); // Larger than Short.MAX_VALUE

        assertEquals(40000, uint16.intValue());
        assertEquals(40000L, uint16.longValue());
        assertEquals(40000.0f, uint16.floatValue(), 0.0f);
        assertEquals(40000.0, uint16.doubleValue(), 0.0);
        assertEquals(
                (short) 40000, uint16.shortValue()); // This will be negative when cast to short
    }

    @Test
    void testNumberMethodsWithExtremeValues() {
        // Test with MIN_VALUE (0)
        DBusUInt16 min = DBusUInt16.valueOf((short) 0);
        assertEquals(0, min.intValue());
        assertEquals(0L, min.longValue());
        assertEquals(0.0f, min.floatValue(), 0.0f);
        assertEquals(0.0, min.doubleValue(), 0.0);

        // Test with MAX_VALUE (65535)
        DBusUInt16 max = DBusUInt16.valueOf((short) 65535);
        assertEquals(65535, max.intValue());
        assertEquals(65535L, max.longValue());
        assertEquals(65535.0f, max.floatValue(), 0.0f);
        assertEquals(65535.0, max.doubleValue(), 0.0);
    }

    @Test
    void testGetDelegate() {
        short value = 12345;
        DBusUInt16 uint16 = DBusUInt16.valueOf(value);

        assertEquals(Short.valueOf(value), uint16.getDelegate());
        assertEquals(value, uint16.getDelegate().shortValue());
    }

    @Test
    void testGetType() {
        assertEquals(Type.UINT16, DBusUInt16.valueOf((short) 0).getType());
        assertEquals(Type.UINT16, DBusUInt16.valueOf((short) 65535).getType());
        assertEquals(Type.UINT16, DBusUInt16.valueOf((short) 32768).getType());
    }

    @Test
    void testBoundaryValues() {
        // Test values at the boundary
        DBusUInt16 minValue = DBusUInt16.valueOf((short) 0);
        DBusUInt16 maxValue = DBusUInt16.valueOf((short) 65535);
        DBusUInt16 minPlusOne = DBusUInt16.valueOf((short) 1);
        DBusUInt16 maxMinusOne = DBusUInt16.valueOf((short) 65534);
        DBusUInt16 signedBoundary =
                DBusUInt16.valueOf((short) 32768); // Where signed short becomes negative

        assertEquals(0, minValue.intValue());
        assertEquals(65535, maxValue.intValue());
        assertEquals(1, minPlusOne.intValue());
        assertEquals(65534, maxMinusOne.intValue());
        assertEquals(32768, signedBoundary.intValue());

        assertTrue(minValue.compareTo(minPlusOne) < 0);
        assertTrue(maxMinusOne.compareTo(maxValue) < 0);
        assertTrue(minValue.compareTo(signedBoundary) < 0);
        assertTrue(signedBoundary.compareTo(maxValue) < 0);
    }

    @Test
    void testSignedShortBoundaryBehavior() {
        // Test behavior at the signed short boundary
        DBusUInt16 maxSigned = DBusUInt16.valueOf((short) 32767); // Short.MAX_VALUE
        DBusUInt16 minUnsigned = DBusUInt16.valueOf((short) 32768); // Where short becomes negative

        assertEquals(32767, maxSigned.intValue());
        assertEquals(32767, maxSigned.shortValue());

        assertEquals(32768, minUnsigned.intValue());
        assertEquals(-32768, minUnsigned.shortValue()); // Wraps to negative

        assertTrue(maxSigned.compareTo(minUnsigned) < 0); // 32767 < 32768 in unsigned comparison
    }

    @Test
    void testImmutability() {
        short original = 12345;
        DBusUInt16 uint16 = DBusUInt16.valueOf(original);

        // Verify the delegate is the expected value
        assertEquals(original, uint16.getDelegate().shortValue());

        // UInt16 should be immutable - no setters to test
        // Just verify that getting the delegate multiple times returns consistent values
        assertEquals(uint16.getDelegate(), uint16.getDelegate());
        assertEquals(uint16.intValue(), uint16.intValue());
    }

    @Test
    void testCompareToIsConsistentWithEquals() {
        DBusUInt16 uint1 = DBusUInt16.valueOf((short) 123);
        DBusUInt16 uint2 = DBusUInt16.valueOf((short) 123);
        DBusUInt16 uint3 = DBusUInt16.valueOf((short) 456);

        // If compareTo returns 0, equals should return true
        assertEquals(0, uint1.compareTo(uint2));
        assertEquals(uint1, uint2);

        // If compareTo returns non-zero, equals should return false
        assertNotEquals(0, uint1.compareTo(uint3));
        assertNotEquals(uint1, uint3);
    }

    @Test
    void testUnsignedComparisonLogic() {
        // Test that comparison works correctly for unsigned values
        DBusUInt16 large1 = DBusUInt16.valueOf((short) 40000); // > Short.MAX_VALUE
        DBusUInt16 large2 = DBusUInt16.valueOf((short) 50000); // > Short.MAX_VALUE
        DBusUInt16 small = DBusUInt16.valueOf((short) 1000); // < Short.MAX_VALUE

        assertTrue(small.compareTo(large1) < 0);
        assertTrue(large1.compareTo(large2) < 0);
        assertTrue(large2.compareTo(small) > 0);
        assertTrue(large2.compareTo(large1) > 0);
    }
}
