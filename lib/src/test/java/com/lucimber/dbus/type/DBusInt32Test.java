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

final class DBusInt32Test {

    @Test
    void createInt32WithZero() {
        DBusInt32 int32 = DBusInt32.valueOf(0);

        assertEquals(0, int32.intValue());
        assertEquals(Type.INT32, int32.getType());
        assertEquals("0", int32.toString());
    }

    @Test
    void createInt32WithPositiveValue() {
        int value = 123456;
        DBusInt32 int32 = DBusInt32.valueOf(value);

        assertEquals(value, int32.intValue());
        assertEquals(Type.INT32, int32.getType());
        assertEquals(Integer.toString(value), int32.toString());
    }

    @Test
    void createInt32WithNegativeValue() {
        int value = -123456;
        DBusInt32 int32 = DBusInt32.valueOf(value);

        assertEquals(value, int32.intValue());
        assertEquals(Type.INT32, int32.getType());
        assertEquals(Integer.toString(value), int32.toString());
    }

    @Test
    void createInt32WithExtremeValues() {
        // Test minimum value
        DBusInt32 minInt32 = DBusInt32.valueOf(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, minInt32.intValue());
        assertEquals(Type.INT32, minInt32.getType());
        assertEquals(Integer.toString(Integer.MIN_VALUE), minInt32.toString());

        // Test maximum value
        DBusInt32 maxInt32 = DBusInt32.valueOf(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, maxInt32.intValue());
        assertEquals(Type.INT32, maxInt32.getType());
        assertEquals(Integer.toString(Integer.MAX_VALUE), maxInt32.toString());
    }

    @ParameterizedTest
    @ValueSource(
            ints = {
                0,
                1,
                -1,
                123456,
                -123456,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                1000000,
                -1000000,
                42,
                -42
            })
    void createInt32WithVariousValues(int value) {
        DBusInt32 int32 = DBusInt32.valueOf(value);

        assertEquals(value, int32.intValue());
        assertEquals(Type.INT32, int32.getType());
        assertEquals(Integer.toString(value), int32.toString());
    }

    @Test
    void testEquals() {
        DBusInt32 int1 = DBusInt32.valueOf(123456);
        DBusInt32 int2 = DBusInt32.valueOf(123456);
        DBusInt32 int3 = DBusInt32.valueOf(123457);

        assertEquals(int1, int2);
        assertNotEquals(int1, int3);
        assertEquals(int1, int1); // self-equality

        assertNotEquals(int1, null);
        assertNotEquals(int1, "123456"); // Different type
        assertNotEquals(int1, 123456); // Different type
    }

    @Test
    void testEqualsWithExtremeValues() {
        DBusInt32 min1 = DBusInt32.valueOf(Integer.MIN_VALUE);
        DBusInt32 min2 = DBusInt32.valueOf(Integer.MIN_VALUE);
        DBusInt32 max1 = DBusInt32.valueOf(Integer.MAX_VALUE);
        DBusInt32 max2 = DBusInt32.valueOf(Integer.MAX_VALUE);

        assertEquals(min1, min2);
        assertEquals(max1, max2);
        assertNotEquals(min1, max1);
    }

    @Test
    void testHashCode() {
        DBusInt32 int1 = DBusInt32.valueOf(123456);
        DBusInt32 int2 = DBusInt32.valueOf(123456);
        DBusInt32 int3 = DBusInt32.valueOf(123457);

        assertEquals(int1.hashCode(), int2.hashCode());
        assertNotEquals(int1.hashCode(), int3.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("0", DBusInt32.valueOf(0).toString());
        assertEquals("123456", DBusInt32.valueOf(123456).toString());
        assertEquals("-123456", DBusInt32.valueOf(-123456).toString());
        assertEquals("2147483647", DBusInt32.valueOf(Integer.MAX_VALUE).toString());
        assertEquals("-2147483648", DBusInt32.valueOf(Integer.MIN_VALUE).toString());
    }

    @Test
    void testCompareTo() {
        DBusInt32 small = DBusInt32.valueOf(-100);
        DBusInt32 zero = DBusInt32.valueOf(0);
        DBusInt32 large = DBusInt32.valueOf(100);
        DBusInt32 duplicate = DBusInt32.valueOf(0);

        assertTrue(small.compareTo(zero) < 0);
        assertTrue(zero.compareTo(large) < 0);
        assertTrue(large.compareTo(zero) > 0);
        assertTrue(zero.compareTo(small) > 0);
        assertEquals(0, zero.compareTo(duplicate));
        assertEquals(0, zero.compareTo(zero));
    }

    @Test
    void testCompareToWithExtremeValues() {
        DBusInt32 min = DBusInt32.valueOf(Integer.MIN_VALUE);
        DBusInt32 max = DBusInt32.valueOf(Integer.MAX_VALUE);
        DBusInt32 zero = DBusInt32.valueOf(0);

        assertTrue(min.compareTo(max) < 0);
        assertTrue(max.compareTo(min) > 0);
        assertTrue(min.compareTo(zero) < 0);
        assertTrue(zero.compareTo(min) > 0);
        assertTrue(zero.compareTo(max) < 0);
        assertTrue(max.compareTo(zero) > 0);
    }

    @Test
    void testCompareToOverflowSafety() {
        // Test that compareTo doesn't overflow when subtracting large values
        DBusInt32 maxValue = DBusInt32.valueOf(Integer.MAX_VALUE);
        DBusInt32 minValue = DBusInt32.valueOf(Integer.MIN_VALUE);

        // This would overflow if using simple subtraction: MAX_VALUE - MIN_VALUE
        assertTrue(maxValue.compareTo(minValue) > 0);
        assertTrue(minValue.compareTo(maxValue) < 0);

        // Test edge cases near overflow boundaries
        DBusInt32 almostMax = DBusInt32.valueOf(Integer.MAX_VALUE - 1);
        DBusInt32 almostMin = DBusInt32.valueOf(Integer.MIN_VALUE + 1);

        assertTrue(maxValue.compareTo(almostMax) > 0);
        assertTrue(almostMin.compareTo(minValue) > 0);
    }

    @Test
    void testNumberMethods() {
        DBusInt32 int32 = DBusInt32.valueOf(123456);

        assertEquals(123456, int32.intValue());
        assertEquals(123456L, int32.longValue());
        assertEquals(123456.0f, int32.floatValue(), 0.0f);
        assertEquals(123456.0, int32.doubleValue(), 0.0);
        assertEquals((byte) 123456, int32.byteValue()); // Truncated
        assertEquals((short) 123456, int32.shortValue()); // Truncated
    }

    @Test
    void testNumberMethodsWithNegativeValues() {
        DBusInt32 negativeInt32 = DBusInt32.valueOf(-123456);

        assertEquals(-123456, negativeInt32.intValue());
        assertEquals(-123456L, negativeInt32.longValue());
        assertEquals(-123456.0f, negativeInt32.floatValue(), 0.0f);
        assertEquals(-123456.0, negativeInt32.doubleValue(), 0.0);
        assertEquals((byte) -123456, negativeInt32.byteValue()); // Truncated
        assertEquals((short) -123456, negativeInt32.shortValue()); // Truncated
    }

    @Test
    void testNumberMethodsWithExtremeValues() {
        // Test with MIN_VALUE
        DBusInt32 minInt32 = DBusInt32.valueOf(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, minInt32.intValue());
        assertEquals((long) Integer.MIN_VALUE, minInt32.longValue());
        assertEquals((float) Integer.MIN_VALUE, minInt32.floatValue(), 0.0f);
        assertEquals((double) Integer.MIN_VALUE, minInt32.doubleValue(), 0.0);

        // Test with MAX_VALUE
        DBusInt32 maxInt32 = DBusInt32.valueOf(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, maxInt32.intValue());
        assertEquals((long) Integer.MAX_VALUE, maxInt32.longValue());
        assertEquals((float) Integer.MAX_VALUE, maxInt32.floatValue(), 0.0f);
        assertEquals((double) Integer.MAX_VALUE, maxInt32.doubleValue(), 0.0);
    }

    @Test
    void testGetDelegate() {
        int value = 123456;
        DBusInt32 int32 = DBusInt32.valueOf(value);

        assertEquals(Integer.valueOf(value), int32.getDelegate());
        assertEquals(value, int32.getDelegate().intValue());
    }

    @Test
    void testGetType() {
        assertEquals(Type.INT32, DBusInt32.valueOf(0).getType());
        assertEquals(Type.INT32, DBusInt32.valueOf(Integer.MAX_VALUE).getType());
        assertEquals(Type.INT32, DBusInt32.valueOf(Integer.MIN_VALUE).getType());
        assertEquals(Type.INT32, DBusInt32.valueOf(-1).getType());
        assertEquals(Type.INT32, DBusInt32.valueOf(1).getType());
    }

    @Test
    void testBoundaryValues() {
        // Test values at integer boundaries
        DBusInt32 minValue = DBusInt32.valueOf(Integer.MIN_VALUE);
        DBusInt32 maxValue = DBusInt32.valueOf(Integer.MAX_VALUE);
        DBusInt32 minPlusOne = DBusInt32.valueOf(Integer.MIN_VALUE + 1);
        DBusInt32 maxMinusOne = DBusInt32.valueOf(Integer.MAX_VALUE - 1);
        DBusInt32 zero = DBusInt32.valueOf(0);

        assertEquals(Integer.MIN_VALUE, minValue.intValue());
        assertEquals(Integer.MAX_VALUE, maxValue.intValue());
        assertEquals(Integer.MIN_VALUE + 1, minPlusOne.intValue());
        assertEquals(Integer.MAX_VALUE - 1, maxMinusOne.intValue());
        assertEquals(0, zero.intValue());

        assertTrue(minValue.compareTo(minPlusOne) < 0);
        assertTrue(maxMinusOne.compareTo(maxValue) < 0);
        assertTrue(minValue.compareTo(zero) < 0);
        assertTrue(zero.compareTo(maxValue) < 0);
    }

    @Test
    void testImmutability() {
        int original = 123456;
        DBusInt32 int32 = DBusInt32.valueOf(original);

        // Verify the delegate is the expected value
        assertEquals(original, int32.getDelegate().intValue());

        // Int32 should be immutable - no setters to test
        // Just verify that getting the delegate multiple times returns consistent values
        assertEquals(int32.getDelegate(), int32.getDelegate());
        assertEquals(int32.intValue(), int32.intValue());
    }

    @Test
    void testCompareToIsConsistentWithEquals() {
        DBusInt32 int1 = DBusInt32.valueOf(123456);
        DBusInt32 int2 = DBusInt32.valueOf(123456);
        DBusInt32 int3 = DBusInt32.valueOf(123457);

        // If compareTo returns 0, equals should return true
        assertEquals(0, int1.compareTo(int2));
        assertEquals(int1, int2);

        // If compareTo returns non-zero, equals should return false
        assertNotEquals(0, int1.compareTo(int3));
        assertNotEquals(int1, int3);
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for INT32 type
        // Per D-Bus specification: INT32 is a 32-bit signed integer

        // Test that full range of 32-bit signed integers is supported
        DBusInt32 minValue = DBusInt32.valueOf(-2147483648); // -2^31
        DBusInt32 maxValue = DBusInt32.valueOf(2147483647); // 2^31 - 1

        assertEquals(-2147483648, minValue.intValue());
        assertEquals(2147483647, maxValue.intValue());
        assertEquals(Type.INT32, minValue.getType());
        assertEquals(Type.INT32, maxValue.getType());

        // Test that comparison works correctly with signed values
        assertTrue(minValue.compareTo(maxValue) < 0);
        assertTrue(maxValue.compareTo(minValue) > 0);

        // Test zero
        DBusInt32 zero = DBusInt32.valueOf(0);
        assertTrue(minValue.compareTo(zero) < 0);
        assertTrue(zero.compareTo(maxValue) < 0);
    }

    @Test
    void testSignedIntegerBehavior() {
        // Test that signed integer behavior is correct
        DBusInt32 positive = DBusInt32.valueOf(1000000);
        DBusInt32 negative = DBusInt32.valueOf(-1000000);
        DBusInt32 zero = DBusInt32.valueOf(0);

        assertTrue(negative.compareTo(zero) < 0);
        assertTrue(zero.compareTo(positive) < 0);
        assertTrue(negative.compareTo(positive) < 0);

        // Test arithmetic boundaries
        DBusInt32 largePositive = DBusInt32.valueOf(2000000000);
        DBusInt32 largeNegative = DBusInt32.valueOf(-2000000000);

        assertTrue(largeNegative.compareTo(largePositive) < 0);
        assertTrue(largePositive.compareTo(zero) > 0);
        assertTrue(largeNegative.compareTo(zero) < 0);
    }

    @Test
    void testPowerOfTwoBoundaries() {
        // Test boundaries at powers of two
        int[] powerOfTwoBoundaries = {
            1,
            2,
            4,
            8,
            16,
            32,
            64,
            128,
            256,
            512,
            1024,
            2048,
            4096,
            8192,
            16384,
            32768,
            65536,
            131072,
            262144,
            524288,
            1048576,
            2097152,
            4194304,
            8388608,
            16777216,
            33554432,
            67108864,
            134217728,
            268435456,
            536870912,
            1073741824
        };

        for (int value : powerOfTwoBoundaries) {
            // Test positive values
            DBusInt32 positive = DBusInt32.valueOf(value);
            assertEquals(value, positive.intValue());
            assertEquals(Type.INT32, positive.getType());

            // Test negative values
            DBusInt32 negative = DBusInt32.valueOf(-value);
            assertEquals(-value, negative.intValue());
            assertEquals(Type.INT32, negative.getType());

            // Test comparison
            assertTrue(negative.compareTo(positive) < 0);
        }
    }

    @Test
    void testCommonValues() {
        // Test commonly used values
        int[] commonValues = {
            0, 1, -1, 10, -10, 100, -100, 1000, -1000, 10000, -10000, 42, -42, 123, -123, 999, -999,
            2020, -2020, 12345, -12345
        };

        for (int value : commonValues) {
            DBusInt32 int32 = DBusInt32.valueOf(value);
            assertEquals(value, int32.intValue());
            assertEquals(Integer.toString(value), int32.toString());
            assertEquals(Type.INT32, int32.getType());
        }
    }
}
