/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class DBusDoubleTest {

    @Test
    void createDoubleWithZero() {
        DBusDouble dbusDouble = DBusDouble.valueOf(0.0);

        assertEquals(0.0, dbusDouble.doubleValue(), 0.0);
        assertEquals(Type.DOUBLE, dbusDouble.getType());
        assertEquals("0.0", dbusDouble.toString());
    }

    @Test
    void createDoubleWithPositiveValue() {
        double value = 123.456;
        DBusDouble dbusDouble = DBusDouble.valueOf(value);

        assertEquals(value, dbusDouble.doubleValue(), 0.0);
        assertEquals(Type.DOUBLE, dbusDouble.getType());
        assertEquals(Double.toString(value), dbusDouble.toString());
    }

    @Test
    void createDoubleWithNegativeValue() {
        double value = -123.456;
        DBusDouble dbusDouble = DBusDouble.valueOf(value);

        assertEquals(value, dbusDouble.doubleValue(), 0.0);
        assertEquals(Type.DOUBLE, dbusDouble.getType());
        assertEquals(Double.toString(value), dbusDouble.toString());
    }

    @Test
    void createDoubleWithExtremeValues() {
        // Test minimum value
        DBusDouble minDouble = DBusDouble.valueOf(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, minDouble.doubleValue(), 0.0);
        assertEquals(Type.DOUBLE, minDouble.getType());

        // Test maximum value
        DBusDouble maxDouble = DBusDouble.valueOf(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, maxDouble.doubleValue(), 0.0);
        assertEquals(Type.DOUBLE, maxDouble.getType());

        // Test minimum normal value
        DBusDouble minNormal = DBusDouble.valueOf(Double.MIN_NORMAL);
        assertEquals(Double.MIN_NORMAL, minNormal.doubleValue(), 0.0);
        assertEquals(Type.DOUBLE, minNormal.getType());
    }

    @ParameterizedTest
    @ValueSource(
            doubles = {
                0.0,
                1.0,
                -1.0,
                123.456,
                -123.456,
                1.7976931348623157E308,
                4.9E-324,
                Math.PI,
                Math.E,
                1.23456789012345E-100,
                1.23456789012345E100
            })
    void createDoubleWithVariousValues(double value) {
        DBusDouble dbusDouble = DBusDouble.valueOf(value);

        assertEquals(value, dbusDouble.doubleValue(), 0.0);
        assertEquals(Type.DOUBLE, dbusDouble.getType());
        assertEquals(Double.toString(value), dbusDouble.toString());
    }

    @Test
    void testSpecialDoubleValues() {
        // Test positive infinity
        DBusDouble posInf = DBusDouble.valueOf(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, posInf.doubleValue(), 0.0);
        assertTrue(Double.isInfinite(posInf.doubleValue()));
        assertTrue(posInf.doubleValue() > 0);

        // Test negative infinity
        DBusDouble negInf = DBusDouble.valueOf(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, negInf.doubleValue(), 0.0);
        assertTrue(Double.isInfinite(negInf.doubleValue()));
        assertTrue(negInf.doubleValue() < 0);

        // Test NaN
        DBusDouble nan = DBusDouble.valueOf(Double.NaN);
        assertEquals(Double.NaN, nan.doubleValue(), 0.0);
        assertTrue(Double.isNaN(nan.doubleValue()));

        // Test positive zero
        DBusDouble posZero = DBusDouble.valueOf(0.0);
        assertEquals(0.0, posZero.doubleValue(), 0.0);
        assertFalse(Double.isNaN(posZero.doubleValue()));
        assertFalse(Double.isInfinite(posZero.doubleValue()));

        // Test negative zero
        DBusDouble negZero = DBusDouble.valueOf(-0.0);
        assertEquals(-0.0, negZero.doubleValue(), 0.0);
        assertFalse(Double.isNaN(negZero.doubleValue()));
        assertFalse(Double.isInfinite(negZero.doubleValue()));
    }

    @Test
    void testEquals() {
        DBusDouble double1 = DBusDouble.valueOf(123.456);
        DBusDouble double2 = DBusDouble.valueOf(123.456);
        DBusDouble double3 = DBusDouble.valueOf(123.457);

        assertEquals(double1, double2);
        assertNotEquals(double1, double3);
        assertEquals(double1, double1); // self-equality

        assertNotEquals(double1, null);
        assertNotEquals(double1, "123.456"); // Different type
        assertNotEquals(double1, 123.456); // Different type
    }

    @Test
    void testEqualsWithSpecialValues() {
        // Test NaN equality (should be equal according to D-Bus spec)
        DBusDouble nan1 = DBusDouble.valueOf(Double.NaN);
        DBusDouble nan2 = DBusDouble.valueOf(Double.NaN);
        assertEquals(nan1, nan2);

        // Test infinity equality
        DBusDouble posInf1 = DBusDouble.valueOf(Double.POSITIVE_INFINITY);
        DBusDouble posInf2 = DBusDouble.valueOf(Double.POSITIVE_INFINITY);
        assertEquals(posInf1, posInf2);

        DBusDouble negInf1 = DBusDouble.valueOf(Double.NEGATIVE_INFINITY);
        DBusDouble negInf2 = DBusDouble.valueOf(Double.NEGATIVE_INFINITY);
        assertEquals(negInf1, negInf2);

        // Test zero equality
        DBusDouble posZero = DBusDouble.valueOf(0.0);
        DBusDouble negZero = DBusDouble.valueOf(-0.0);
        // Double.compare treats +0.0 and -0.0 as different
        assertNotEquals(posZero, negZero);
    }

    @Test
    void testHashCode() {
        DBusDouble double1 = DBusDouble.valueOf(123.456);
        DBusDouble double2 = DBusDouble.valueOf(123.456);
        DBusDouble double3 = DBusDouble.valueOf(123.457);

        assertEquals(double1.hashCode(), double2.hashCode());
        assertNotEquals(double1.hashCode(), double3.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("0.0", DBusDouble.valueOf(0.0).toString());
        assertEquals("123.456", DBusDouble.valueOf(123.456).toString());
        assertEquals("-123.456", DBusDouble.valueOf(-123.456).toString());
        assertEquals("Infinity", DBusDouble.valueOf(Double.POSITIVE_INFINITY).toString());
        assertEquals("-Infinity", DBusDouble.valueOf(Double.NEGATIVE_INFINITY).toString());
        assertEquals("NaN", DBusDouble.valueOf(Double.NaN).toString());
    }

    @Test
    void testCompareTo() {
        DBusDouble small = DBusDouble.valueOf(1.0);
        DBusDouble medium = DBusDouble.valueOf(2.0);
        DBusDouble large = DBusDouble.valueOf(3.0);
        DBusDouble duplicate = DBusDouble.valueOf(2.0);

        assertTrue(small.compareTo(medium) < 0);
        assertTrue(medium.compareTo(large) < 0);
        assertTrue(large.compareTo(medium) > 0);
        assertTrue(medium.compareTo(small) > 0);
        assertEquals(0, medium.compareTo(duplicate));
        assertEquals(0, medium.compareTo(medium));
    }

    @Test
    void testCompareToWithSpecialValues() {
        DBusDouble normal = DBusDouble.valueOf(1.0);
        DBusDouble posInf = DBusDouble.valueOf(Double.POSITIVE_INFINITY);
        DBusDouble negInf = DBusDouble.valueOf(Double.NEGATIVE_INFINITY);
        DBusDouble nan = DBusDouble.valueOf(Double.NaN);
        DBusDouble zero = DBusDouble.valueOf(0.0);

        // NaN comparisons
        assertTrue(nan.compareTo(normal) > 0);
        assertTrue(nan.compareTo(posInf) > 0);
        assertTrue(nan.compareTo(negInf) > 0);
        assertEquals(0, nan.compareTo(nan));

        // Infinity comparisons
        assertTrue(posInf.compareTo(normal) > 0);
        assertTrue(negInf.compareTo(normal) < 0);
        assertTrue(posInf.compareTo(negInf) > 0);

        // Zero comparisons
        assertTrue(zero.compareTo(normal) < 0);
        assertTrue(normal.compareTo(zero) > 0);
    }

    @Test
    void testNumberMethods() {
        DBusDouble dbusDouble = DBusDouble.valueOf(123.456);

        assertEquals(123, dbusDouble.intValue());
        assertEquals(123L, dbusDouble.longValue());
        assertEquals(123.456f, dbusDouble.floatValue(), 0.001f);
        assertEquals(123.456, dbusDouble.doubleValue(), 0.0);
        assertEquals((byte) 123, dbusDouble.byteValue());
        assertEquals((short) 123, dbusDouble.shortValue());
    }

    @Test
    void testNumberMethodsWithLargeValues() {
        // Test with very large double
        DBusDouble largeDouble = DBusDouble.valueOf(1.7976931348623157E308);

        assertEquals(Double.MAX_VALUE, largeDouble.doubleValue(), 0.0);
        assertEquals(Integer.MAX_VALUE, largeDouble.intValue()); // Clamped to int max
        assertEquals(Long.MAX_VALUE, largeDouble.longValue()); // Clamped to long max
        assertEquals(Float.POSITIVE_INFINITY, largeDouble.floatValue(), 0.0f);

        // Test with very small double
        DBusDouble smallDouble = DBusDouble.valueOf(4.9E-324);

        assertEquals(Double.MIN_VALUE, smallDouble.doubleValue(), 0.0);
        assertEquals(0, smallDouble.intValue()); // Truncated to 0
        assertEquals(0L, smallDouble.longValue()); // Truncated to 0
        assertEquals(0.0f, smallDouble.floatValue(), 0.0f); // Underflow to 0
    }

    @Test
    void testNumberMethodsWithSpecialValues() {
        // Test infinity
        DBusDouble posInf = DBusDouble.valueOf(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, posInf.doubleValue(), 0.0);
        assertEquals(Integer.MAX_VALUE, posInf.intValue());
        assertEquals(Long.MAX_VALUE, posInf.longValue());
        assertEquals(Float.POSITIVE_INFINITY, posInf.floatValue(), 0.0f);

        DBusDouble negInf = DBusDouble.valueOf(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, negInf.doubleValue(), 0.0);
        assertEquals(Integer.MIN_VALUE, negInf.intValue());
        assertEquals(Long.MIN_VALUE, negInf.longValue());
        assertEquals(Float.NEGATIVE_INFINITY, negInf.floatValue(), 0.0f);

        // Test NaN
        DBusDouble nan = DBusDouble.valueOf(Double.NaN);
        assertEquals(Double.NaN, nan.doubleValue(), 0.0);
        assertEquals(0, nan.intValue());
        assertEquals(0L, nan.longValue());
        assertTrue(Float.isNaN(nan.floatValue()));
    }

    @Test
    void testGetDelegate() {
        double value = 123.456;
        DBusDouble dbusDouble = DBusDouble.valueOf(value);

        assertEquals(Double.valueOf(value), dbusDouble.getDelegate());
        assertEquals(value, dbusDouble.getDelegate().doubleValue(), 0.0);
    }

    @Test
    void testGetType() {
        assertEquals(Type.DOUBLE, DBusDouble.valueOf(0.0).getType());
        assertEquals(Type.DOUBLE, DBusDouble.valueOf(Double.MAX_VALUE).getType());
        assertEquals(Type.DOUBLE, DBusDouble.valueOf(Double.MIN_VALUE).getType());
        assertEquals(Type.DOUBLE, DBusDouble.valueOf(Double.POSITIVE_INFINITY).getType());
        assertEquals(Type.DOUBLE, DBusDouble.valueOf(Double.NaN).getType());
    }

    @Test
    void testImmutability() {
        double original = 123.456;
        DBusDouble dbusDouble = DBusDouble.valueOf(original);

        // Verify the delegate is the expected value
        assertEquals(original, dbusDouble.getDelegate().doubleValue(), 0.0);

        // DBusDouble should be immutable - no setters to test
        // Just verify that getting the delegate multiple times returns consistent values
        assertEquals(dbusDouble.getDelegate(), dbusDouble.getDelegate());
        assertEquals(dbusDouble.doubleValue(), dbusDouble.doubleValue(), 0.0);
    }

    @Test
    void testCompareToIsConsistentWithEquals() {
        DBusDouble double1 = DBusDouble.valueOf(123.456);
        DBusDouble double2 = DBusDouble.valueOf(123.456);
        DBusDouble double3 = DBusDouble.valueOf(123.457);

        // If compareTo returns 0, equals should return true
        assertEquals(0, double1.compareTo(double2));
        assertEquals(double1, double2);

        // If compareTo returns non-zero, equals should return false
        assertNotEquals(0, double1.compareTo(double3));
        assertNotEquals(double1, double3);
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for DOUBLE type
        // Per D-Bus specification: DOUBLE is IEEE 754 double-precision floating point

        // Test that IEEE 754 special values are supported
        assertDoesNotThrow(
                () -> {
                    DBusDouble.valueOf(Double.POSITIVE_INFINITY);
                    DBusDouble.valueOf(Double.NEGATIVE_INFINITY);
                    DBusDouble.valueOf(Double.NaN);
                    DBusDouble.valueOf(0.0);
                    DBusDouble.valueOf(-0.0);
                });

        // Test that precision is maintained
        double precisionTest = 1.23456789012345678901234567890;
        DBusDouble dbusDouble = DBusDouble.valueOf(precisionTest);
        assertEquals(precisionTest, dbusDouble.doubleValue(), 0.0);

        // Test that type is correctly identified
        assertEquals(Type.DOUBLE, dbusDouble.getType());
    }

    @Test
    void testPrecisionAndAccuracy() {
        // Test various precision scenarios
        double[] precisionTests = {
            1.0,
            0.1,
            0.01,
            0.001,
            0.0001,
            0.00001,
            0.000001,
            0.0000001,
            0.00000001,
            0.000000001,
            0.0000000001,
            0.00000000001,
            0.000000000001,
            0.0000000000001,
            0.00000000000001,
            0.000000000000001,
            1.7976931348623157E308, // Max value
            4.9E-324, // Min value
            Math.PI,
            Math.E,
            Double.longBitsToDouble(0x7fefffffffffffffL), // Largest finite value
            Double.longBitsToDouble(0x0000000000000001L) // Smallest positive value
        };

        for (double value : precisionTests) {
            DBusDouble dbusDouble = DBusDouble.valueOf(value);
            assertEquals(
                    value, dbusDouble.doubleValue(), 0.0, "Precision lost for value: " + value);
        }
    }

    @Test
    void testNegativeZeroHandling() {
        // Test that positive and negative zero are handled correctly
        DBusDouble posZero = DBusDouble.valueOf(0.0);
        DBusDouble negZero = DBusDouble.valueOf(-0.0);

        // They should be different per Double.compare behavior
        assertNotEquals(posZero, negZero);

        // But maintain their bit representation
        assertEquals(0.0, posZero.doubleValue(), 0.0);
        assertEquals(-0.0, negZero.doubleValue(), 0.0);

        // Test that they compare as different
        assertTrue(posZero.compareTo(negZero) > 0);
    }
}
