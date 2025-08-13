/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class DBusUInt64Test {

    @Test
    void createUInt64WithZero() {
        DBusUInt64 uint64 = DBusUInt64.valueOf(0L);

        assertEquals(0L, uint64.longValue());
        assertEquals(Type.UINT64, uint64.getType());
        assertEquals("0", uint64.toString());
    }

    @Test
    void createUInt64WithPositiveValue() {
        long value = 123456789012345L;
        DBusUInt64 uint64 = DBusUInt64.valueOf(value);

        assertEquals(value, uint64.longValue());
        assertEquals(Type.UINT64, uint64.getType());
        assertEquals(Long.toUnsignedString(value), uint64.toString());
    }

    @Test
    void createUInt64WithLargeUnsignedValue() {
        // Test with a value that would be negative in signed representation
        long value = -1L; // Represents 18446744073709551615 in unsigned
        DBusUInt64 uint64 = DBusUInt64.valueOf(value);

        assertEquals(value, uint64.longValue());
        assertEquals(Type.UINT64, uint64.getType());
        assertEquals("18446744073709551615", uint64.toString());
    }

    @Test
    void createUInt64WithMaxValue() {
        // Test maximum unsigned value (represented as -1 in signed long)
        long maxUnsigned = -1L;
        DBusUInt64 uint64 = DBusUInt64.valueOf(maxUnsigned);

        assertEquals(maxUnsigned, uint64.longValue());
        assertEquals(Type.UINT64, uint64.getType());
        assertEquals("18446744073709551615", uint64.toString());
    }

    @ParameterizedTest
    @ValueSource(
            longs = {
                0L,
                1L,
                255L,
                256L,
                65535L,
                65536L,
                4294967295L,
                4294967296L,
                Long.MAX_VALUE,
                -1L,
                -100L,
                -9223372036854775808L
            })
    void createUInt64WithVariousValues(long value) {
        DBusUInt64 uint64 = DBusUInt64.valueOf(value);

        assertEquals(value, uint64.longValue());
        assertEquals(Type.UINT64, uint64.getType());
        assertEquals(Long.toUnsignedString(value), uint64.toString());
    }

    @Test
    void testEquals() {
        DBusUInt64 uint1 = DBusUInt64.valueOf(123456789012345L);
        DBusUInt64 uint2 = DBusUInt64.valueOf(123456789012345L);
        DBusUInt64 uint3 = DBusUInt64.valueOf(123456789012346L);

        assertEquals(uint1, uint2);
        assertNotEquals(uint1, uint3);
        assertEquals(uint1, uint1); // self-equality

        assertNotEquals(uint1, null);
        assertNotEquals(uint1, "123456789012345"); // Different type
        assertNotEquals(uint1, 123456789012345L); // Different type
    }

    @Test
    void testEqualsWithUnsignedValues() {
        DBusUInt64 uint1 = DBusUInt64.valueOf(-1L); // Max unsigned value
        DBusUInt64 uint2 = DBusUInt64.valueOf(-1L);
        DBusUInt64 uint3 = DBusUInt64.valueOf(-2L);

        assertEquals(uint1, uint2);
        assertNotEquals(uint1, uint3);
    }

    @Test
    void testHashCode() {
        DBusUInt64 uint1 = DBusUInt64.valueOf(123456789012345L);
        DBusUInt64 uint2 = DBusUInt64.valueOf(123456789012345L);
        DBusUInt64 uint3 = DBusUInt64.valueOf(123456789012346L);

        assertEquals(uint1.hashCode(), uint2.hashCode());
        assertNotEquals(uint1.hashCode(), uint3.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("0", DBusUInt64.valueOf(0L).toString());
        assertEquals("123456789012345", DBusUInt64.valueOf(123456789012345L).toString());
        assertEquals("9223372036854775807", DBusUInt64.valueOf(Long.MAX_VALUE).toString());
        assertEquals("18446744073709551615", DBusUInt64.valueOf(-1L).toString()); // Max unsigned
        assertEquals(
                "9223372036854775808",
                DBusUInt64.valueOf(Long.MIN_VALUE).toString()); // 2^63 unsigned
    }

    @Test
    void testCompareTo() {
        DBusUInt64 small = DBusUInt64.valueOf(100L);
        DBusUInt64 medium = DBusUInt64.valueOf(1000L);
        DBusUInt64 large = DBusUInt64.valueOf(10000L);
        DBusUInt64 duplicate = DBusUInt64.valueOf(1000L);

        assertTrue(small.compareTo(medium) < 0);
        assertTrue(medium.compareTo(large) < 0);
        assertTrue(large.compareTo(medium) > 0);
        assertTrue(medium.compareTo(small) > 0);
        assertEquals(0, medium.compareTo(duplicate));
        assertEquals(0, medium.compareTo(medium));
    }

    @Test
    void testCompareToWithUnsignedValues() {
        DBusUInt64 zero = DBusUInt64.valueOf(0L);
        DBusUInt64 signedMax = DBusUInt64.valueOf(Long.MAX_VALUE);
        DBusUInt64 signedMin = DBusUInt64.valueOf(Long.MIN_VALUE); // 2^63 in unsigned
        DBusUInt64 unsignedMax = DBusUInt64.valueOf(-1L); // Max unsigned value

        // In unsigned comparison:
        // 0 < Long.MAX_VALUE < 2^63 < UINT64_MAX
        assertTrue(zero.compareTo(signedMax) < 0);
        assertTrue(signedMax.compareTo(signedMin) < 0);
        assertTrue(signedMin.compareTo(unsignedMax) < 0);

        // Reverse comparisons
        assertTrue(signedMax.compareTo(zero) > 0);
        assertTrue(signedMin.compareTo(signedMax) > 0);
        assertTrue(unsignedMax.compareTo(signedMin) > 0);
    }

    @Test
    void testUnsignedComparison() {
        // Test that unsigned comparison works correctly
        DBusUInt64 largeUnsigned1 = DBusUInt64.valueOf(-100L); // Very large unsigned value
        DBusUInt64 largeUnsigned2 = DBusUInt64.valueOf(-50L); // Smaller but still large unsigned
        DBusUInt64 smallPositive = DBusUInt64.valueOf(100L);

        // In unsigned terms: -100 (as unsigned) < -50 (as unsigned), but both > 100
        assertTrue(largeUnsigned1.compareTo(largeUnsigned2) < 0);
        assertTrue(largeUnsigned2.compareTo(smallPositive) > 0);
        assertTrue(largeUnsigned1.compareTo(smallPositive) > 0);
    }

    @Test
    void testNumberMethods() {
        DBusUInt64 uint64 = DBusUInt64.valueOf(123456789012345L);

        assertEquals((int) 123456789012345L, uint64.intValue()); // Truncated
        assertEquals(123456789012345L, uint64.longValue());
        assertEquals(123456789012345.0f, uint64.floatValue(), 0.0f);
        assertEquals(123456789012345.0, uint64.doubleValue(), 0.0);
        assertEquals((byte) 123456789012345L, uint64.byteValue()); // Truncated
        assertEquals((short) 123456789012345L, uint64.shortValue()); // Truncated
    }

    @Test
    void testNumberMethodsWithUnsignedValues() {
        // Test with maximum unsigned value (-1 in signed representation)
        DBusUInt64 maxUnsigned = DBusUInt64.valueOf(-1L);

        assertEquals(-1, maxUnsigned.intValue()); // Truncated to signed int
        assertEquals(-1L, maxUnsigned.longValue()); // Raw signed representation

        // Float and double should represent the unsigned value
        assertEquals(18446744073709551615.0f, maxUnsigned.floatValue(), 0.0f);
        assertEquals(18446744073709551615.0, maxUnsigned.doubleValue(), 0.0);

        // Test with 2^63 (Long.MIN_VALUE in signed representation)
        DBusUInt64 twoTo63 = DBusUInt64.valueOf(Long.MIN_VALUE);
        assertEquals(9223372036854775808.0f, twoTo63.floatValue(), 0.0f);
        assertEquals(9223372036854775808.0, twoTo63.doubleValue(), 0.0);
    }

    @Test
    void testGetDelegate() {
        long value = 123456789012345L;
        DBusUInt64 uint64 = DBusUInt64.valueOf(value);

        assertEquals(Long.valueOf(value), uint64.getDelegate());
        assertEquals(value, uint64.getDelegate().longValue());
    }

    @Test
    void testGetType() {
        assertEquals(Type.UINT64, DBusUInt64.valueOf(0L).getType());
        assertEquals(Type.UINT64, DBusUInt64.valueOf(Long.MAX_VALUE).getType());
        assertEquals(Type.UINT64, DBusUInt64.valueOf(Long.MIN_VALUE).getType());
        assertEquals(Type.UINT64, DBusUInt64.valueOf(-1L).getType());
        assertEquals(Type.UINT64, DBusUInt64.valueOf(1L).getType());
    }

    @Test
    void testBoundaryValues() {
        // Test values at unsigned 64-bit boundaries
        DBusUInt64 zero = DBusUInt64.valueOf(0L);
        DBusUInt64 one = DBusUInt64.valueOf(1L);
        DBusUInt64 signedMax = DBusUInt64.valueOf(Long.MAX_VALUE);
        DBusUInt64 signedMaxPlusOne = DBusUInt64.valueOf(Long.MIN_VALUE); // 2^63
        DBusUInt64 unsignedMaxMinusOne = DBusUInt64.valueOf(-2L);
        DBusUInt64 unsignedMax = DBusUInt64.valueOf(-1L);

        assertEquals(0L, zero.longValue());
        assertEquals(1L, one.longValue());
        assertEquals(Long.MAX_VALUE, signedMax.longValue());
        assertEquals(Long.MIN_VALUE, signedMaxPlusOne.longValue());
        assertEquals(-2L, unsignedMaxMinusOne.longValue());
        assertEquals(-1L, unsignedMax.longValue());

        // Test unsigned ordering
        assertTrue(zero.compareTo(one) < 0);
        assertTrue(one.compareTo(signedMax) < 0);
        assertTrue(signedMax.compareTo(signedMaxPlusOne) < 0);
        assertTrue(signedMaxPlusOne.compareTo(unsignedMaxMinusOne) < 0);
        assertTrue(unsignedMaxMinusOne.compareTo(unsignedMax) < 0);
    }

    @Test
    void testImmutability() {
        long original = 123456789012345L;
        DBusUInt64 uint64 = DBusUInt64.valueOf(original);

        // Verify the delegate is the expected value
        assertEquals(original, uint64.getDelegate().longValue());

        // UInt64 should be immutable - no setters to test
        // Just verify that getting the delegate multiple times returns consistent values
        assertEquals(uint64.getDelegate(), uint64.getDelegate());
        assertEquals(uint64.longValue(), uint64.longValue());
    }

    @Test
    void testCompareToIsConsistentWithEquals() {
        DBusUInt64 uint1 = DBusUInt64.valueOf(123456789012345L);
        DBusUInt64 uint2 = DBusUInt64.valueOf(123456789012345L);
        DBusUInt64 uint3 = DBusUInt64.valueOf(123456789012346L);

        // If compareTo returns 0, equals should return true
        assertEquals(0, uint1.compareTo(uint2));
        assertEquals(uint1, uint2);

        // If compareTo returns non-zero, equals should return false
        assertNotEquals(0, uint1.compareTo(uint3));
        assertNotEquals(uint1, uint3);
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for UINT64 type
        // Per D-Bus specification: UINT64 is a 64-bit unsigned integer

        // Test that full range of 64-bit unsigned integers is supported
        DBusUInt64 minValue = DBusUInt64.valueOf(0L);
        DBusUInt64 maxValue = DBusUInt64.valueOf(-1L); // 18446744073709551615 (2^64 - 1)

        assertEquals(0L, minValue.longValue());
        assertEquals(-1L, maxValue.longValue()); // Raw signed representation
        assertEquals("0", minValue.toString());
        assertEquals("18446744073709551615", maxValue.toString());
        assertEquals(Type.UINT64, minValue.getType());
        assertEquals(Type.UINT64, maxValue.getType());

        // Test that unsigned comparison works correctly
        assertTrue(minValue.compareTo(maxValue) < 0);
        assertTrue(maxValue.compareTo(minValue) > 0);

        // Test middle values
        DBusUInt64 signedMax = DBusUInt64.valueOf(Long.MAX_VALUE);
        DBusUInt64 signedMin = DBusUInt64.valueOf(Long.MIN_VALUE); // 2^63 unsigned

        assertTrue(minValue.compareTo(signedMax) < 0);
        assertTrue(signedMax.compareTo(signedMin) < 0);
        assertTrue(signedMin.compareTo(maxValue) < 0);
    }

    @Test
    void testUnsignedIntegerBehavior() {
        // Test that unsigned integer behavior is correct
        DBusUInt64 zero = DBusUInt64.valueOf(0L);
        DBusUInt64 small = DBusUInt64.valueOf(1000L);
        DBusUInt64 large = DBusUInt64.valueOf(Long.MAX_VALUE);
        DBusUInt64 veryLarge = DBusUInt64.valueOf(Long.MIN_VALUE); // 2^63 unsigned
        DBusUInt64 maximum = DBusUInt64.valueOf(-1L); // Max unsigned

        // Test ordering
        assertTrue(zero.compareTo(small) < 0);
        assertTrue(small.compareTo(large) < 0);
        assertTrue(large.compareTo(veryLarge) < 0);
        assertTrue(veryLarge.compareTo(maximum) < 0);

        // Test string representations
        assertEquals("0", zero.toString());
        assertEquals("1000", small.toString());
        assertEquals("9223372036854775807", large.toString());
        assertEquals("9223372036854775808", veryLarge.toString());
        assertEquals("18446744073709551615", maximum.toString());
    }

    @Test
    void testPowerOfTwoBoundaries() {
        // Test boundaries at powers of two for unsigned 64-bit values
        long[] powerOfTwoBoundaries = {
            1L,
            2L,
            4L,
            8L,
            16L,
            32L,
            64L,
            128L,
            256L,
            512L,
            1024L,
            2048L,
            4096L,
            8192L,
            16384L,
            32768L,
            65536L,
            131072L,
            262144L,
            524288L,
            1048576L,
            2097152L,
            4194304L,
            8388608L,
            16777216L,
            33554432L,
            67108864L,
            134217728L,
            268435456L,
            536870912L,
            1073741824L,
            2147483648L,
            4294967296L,
            8589934592L,
            17179869184L,
            34359738368L,
            68719476736L,
            137438953472L,
            274877906944L,
            549755813888L,
            1099511627776L,
            2199023255552L,
            4398046511104L,
            8796093022208L,
            17592186044416L,
            35184372088832L,
            70368744177664L,
            140737488355328L,
            281474976710656L,
            562949953421312L,
            1125899906842624L,
            2251799813685248L,
            4503599627370496L,
            9007199254740992L,
            18014398509481984L,
            36028797018963968L,
            72057594037927936L,
            144115188075855872L,
            288230376151711744L,
            576460752303423488L,
            1152921504606846976L,
            2305843009213693952L,
            4611686018427387904L,
            Long.MIN_VALUE // 2^63
        };

        for (long value : powerOfTwoBoundaries) {
            DBusUInt64 uint64 = DBusUInt64.valueOf(value);
            assertEquals(value, uint64.longValue());
            assertEquals(Type.UINT64, uint64.getType());
            assertEquals(Long.toUnsignedString(value), uint64.toString());
        }
    }

    @Test
    void testCommonValues() {
        // Test commonly used values
        long[] commonValues = {
            0L,
            1L,
            10L,
            100L,
            1000L,
            10000L,
            100000L,
            1000000L,
            10000000L,
            100000000L,
            1000000000L,
            42L,
            123L,
            255L,
            256L,
            999L,
            1024L,
            2020L,
            12345L,
            65535L,
            65536L,
            4294967295L,
            4294967296L,
            Long.MAX_VALUE,
            Long.MIN_VALUE,
            -1L,
            -2L,
            -100L,
            -1000L
        };

        for (long value : commonValues) {
            DBusUInt64 uint64 = DBusUInt64.valueOf(value);
            assertEquals(value, uint64.longValue());
            assertEquals(Long.toUnsignedString(value), uint64.toString());
            assertEquals(Type.UINT64, uint64.getType());
        }
    }

    @Test
    void testFloatPrecisionWithUnsignedValues() {
        // Test precision issues with large unsigned values
        DBusUInt64 maxUnsigned = DBusUInt64.valueOf(-1L);

        // Float has limited precision
        float floatValue = maxUnsigned.floatValue();
        assertEquals(18446744073709551615.0f, floatValue, 0.0f);

        // Double should have better precision
        double doubleValue = maxUnsigned.doubleValue();
        assertEquals(18446744073709551615.0, doubleValue, 0.0);
    }

    @Test
    void testIntegerTruncation() {
        // Test that converting large unsigned values to smaller types truncates properly
        DBusUInt64 largeUnsigned = DBusUInt64.valueOf(-1L); // Max unsigned value

        // Should truncate to -1 (all bits set in lower 32 bits)
        assertEquals(-1, largeUnsigned.intValue());
        assertEquals((byte) -1, largeUnsigned.byteValue());
        assertEquals((short) -1, largeUnsigned.shortValue());

        // Test with specific bit patterns
        DBusUInt64 specificValue = DBusUInt64.valueOf(0x123456789ABCDEF0L);
        assertEquals((int) 0x9ABCDEF0, specificValue.intValue());
        assertEquals((byte) 0xF0, specificValue.byteValue());
        assertEquals((short) 0xDEF0, specificValue.shortValue());
    }

    @Test
    void testOverflowSafetyInComparison() {
        // Test that unsigned comparison handles all edge cases safely
        DBusUInt64 zero = DBusUInt64.valueOf(0L);
        DBusUInt64 one = DBusUInt64.valueOf(1L);
        DBusUInt64 signedMax = DBusUInt64.valueOf(Long.MAX_VALUE);
        DBusUInt64 signedMin = DBusUInt64.valueOf(Long.MIN_VALUE);
        DBusUInt64 unsignedMax = DBusUInt64.valueOf(-1L);
        DBusUInt64 almostMax = DBusUInt64.valueOf(-2L);

        // Test all combinations to ensure no overflow in comparison
        assertTrue(zero.compareTo(one) < 0);
        assertTrue(one.compareTo(signedMax) < 0);
        assertTrue(signedMax.compareTo(signedMin) < 0);
        assertTrue(signedMin.compareTo(almostMax) < 0);
        assertTrue(almostMax.compareTo(unsignedMax) < 0);

        // Test reverse comparisons
        assertTrue(unsignedMax.compareTo(almostMax) > 0);
        assertTrue(almostMax.compareTo(signedMin) > 0);
        assertTrue(signedMin.compareTo(signedMax) > 0);
        assertTrue(signedMax.compareTo(one) > 0);
        assertTrue(one.compareTo(zero) > 0);
    }
}
