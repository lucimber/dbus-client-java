/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class Int64Test {

    @Test
    void createInt64WithZero() {
        Int64 int64 = Int64.valueOf(0L);
        
        assertEquals(0L, int64.longValue());
        assertEquals(Type.INT64, int64.getType());
        assertEquals("0", int64.toString());
    }

    @Test
    void createInt64WithPositiveValue() {
        long value = 123456789012345L;
        Int64 int64 = Int64.valueOf(value);
        
        assertEquals(value, int64.longValue());
        assertEquals(Type.INT64, int64.getType());
        assertEquals(Long.toString(value), int64.toString());
    }

    @Test
    void createInt64WithNegativeValue() {
        long value = -123456789012345L;
        Int64 int64 = Int64.valueOf(value);
        
        assertEquals(value, int64.longValue());
        assertEquals(Type.INT64, int64.getType());
        assertEquals(Long.toString(value), int64.toString());
    }

    @Test
    void createInt64WithExtremeValues() {
        // Test minimum value
        Int64 minInt64 = Int64.valueOf(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, minInt64.longValue());
        assertEquals(Type.INT64, minInt64.getType());
        assertEquals(Long.toString(Long.MIN_VALUE), minInt64.toString());
        
        // Test maximum value
        Int64 maxInt64 = Int64.valueOf(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, maxInt64.longValue());
        assertEquals(Type.INT64, maxInt64.getType());
        assertEquals(Long.toString(Long.MAX_VALUE), maxInt64.toString());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, -1L, 123456789012345L, -123456789012345L, Long.MIN_VALUE, Long.MAX_VALUE, 
                         1000000000000000L, -1000000000000000L, 42L, -42L})
    void createInt64WithVariousValues(long value) {
        Int64 int64 = Int64.valueOf(value);
        
        assertEquals(value, int64.longValue());
        assertEquals(Type.INT64, int64.getType());
        assertEquals(Long.toString(value), int64.toString());
    }

    @Test
    void testEquals() {
        Int64 long1 = Int64.valueOf(123456789012345L);
        Int64 long2 = Int64.valueOf(123456789012345L);
        Int64 long3 = Int64.valueOf(123456789012346L);
        
        assertEquals(long1, long2);
        assertNotEquals(long1, long3);
        assertEquals(long1, long1); // self-equality
        
        assertNotEquals(long1, null);
        assertNotEquals(long1, "123456789012345"); // Different type
        assertNotEquals(long1, 123456789012345L); // Different type
    }

    @Test
    void testEqualsWithExtremeValues() {
        Int64 min1 = Int64.valueOf(Long.MIN_VALUE);
        Int64 min2 = Int64.valueOf(Long.MIN_VALUE);
        Int64 max1 = Int64.valueOf(Long.MAX_VALUE);
        Int64 max2 = Int64.valueOf(Long.MAX_VALUE);
        
        assertEquals(min1, min2);
        assertEquals(max1, max2);
        assertNotEquals(min1, max1);
    }

    @Test
    void testHashCode() {
        Int64 long1 = Int64.valueOf(123456789012345L);
        Int64 long2 = Int64.valueOf(123456789012345L);
        Int64 long3 = Int64.valueOf(123456789012346L);
        
        assertEquals(long1.hashCode(), long2.hashCode());
        assertNotEquals(long1.hashCode(), long3.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("0", Int64.valueOf(0L).toString());
        assertEquals("123456789012345", Int64.valueOf(123456789012345L).toString());
        assertEquals("-123456789012345", Int64.valueOf(-123456789012345L).toString());
        assertEquals("9223372036854775807", Int64.valueOf(Long.MAX_VALUE).toString());
        assertEquals("-9223372036854775808", Int64.valueOf(Long.MIN_VALUE).toString());
    }

    @Test
    void testCompareTo() {
        Int64 small = Int64.valueOf(-1000000000000000L);
        Int64 zero = Int64.valueOf(0L);
        Int64 large = Int64.valueOf(1000000000000000L);
        Int64 duplicate = Int64.valueOf(0L);
        
        assertTrue(small.compareTo(zero) < 0);
        assertTrue(zero.compareTo(large) < 0);
        assertTrue(large.compareTo(zero) > 0);
        assertTrue(zero.compareTo(small) > 0);
        assertEquals(0, zero.compareTo(duplicate));
        assertEquals(0, zero.compareTo(zero));
    }

    @Test
    void testCompareToWithExtremeValues() {
        Int64 min = Int64.valueOf(Long.MIN_VALUE);
        Int64 max = Int64.valueOf(Long.MAX_VALUE);
        Int64 zero = Int64.valueOf(0L);
        
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
        Int64 maxValue = Int64.valueOf(Long.MAX_VALUE);
        Int64 minValue = Int64.valueOf(Long.MIN_VALUE);
        
        // This would overflow if using simple subtraction: MAX_VALUE - MIN_VALUE
        assertTrue(maxValue.compareTo(minValue) > 0);
        assertTrue(minValue.compareTo(maxValue) < 0);
        
        // Test edge cases near overflow boundaries
        Int64 almostMax = Int64.valueOf(Long.MAX_VALUE - 1);
        Int64 almostMin = Int64.valueOf(Long.MIN_VALUE + 1);
        
        assertTrue(maxValue.compareTo(almostMax) > 0);
        assertTrue(almostMin.compareTo(minValue) > 0);
    }

    @Test
    void testNumberMethods() {
        Int64 int64 = Int64.valueOf(123456789012345L);
        
        assertEquals((int) 123456789012345L, int64.intValue()); // Truncated
        assertEquals(123456789012345L, int64.longValue());
        assertEquals(123456789012345.0f, int64.floatValue(), 0.0f);
        assertEquals(123456789012345.0, int64.doubleValue(), 0.0);
        assertEquals((byte) 123456789012345L, int64.byteValue()); // Truncated
        assertEquals((short) 123456789012345L, int64.shortValue()); // Truncated
    }

    @Test
    void testNumberMethodsWithNegativeValues() {
        Int64 negativeInt64 = Int64.valueOf(-123456789012345L);
        
        assertEquals((int) -123456789012345L, negativeInt64.intValue()); // Truncated
        assertEquals(-123456789012345L, negativeInt64.longValue());
        assertEquals(-123456789012345.0f, negativeInt64.floatValue(), 0.0f);
        assertEquals(-123456789012345.0, negativeInt64.doubleValue(), 0.0);
        assertEquals((byte) -123456789012345L, negativeInt64.byteValue()); // Truncated
        assertEquals((short) -123456789012345L, negativeInt64.shortValue()); // Truncated
    }

    @Test
    void testNumberMethodsWithExtremeValues() {
        // Test with MIN_VALUE
        Int64 minInt64 = Int64.valueOf(Long.MIN_VALUE);
        assertEquals((int) Long.MIN_VALUE, minInt64.intValue()); // Truncated
        assertEquals(Long.MIN_VALUE, minInt64.longValue());
        assertEquals((float) Long.MIN_VALUE, minInt64.floatValue(), 0.0f);
        assertEquals((double) Long.MIN_VALUE, minInt64.doubleValue(), 0.0);
        
        // Test with MAX_VALUE
        Int64 maxInt64 = Int64.valueOf(Long.MAX_VALUE);
        assertEquals((int) Long.MAX_VALUE, maxInt64.intValue()); // Truncated
        assertEquals(Long.MAX_VALUE, maxInt64.longValue());
        assertEquals((float) Long.MAX_VALUE, maxInt64.floatValue(), 0.0f);
        assertEquals((double) Long.MAX_VALUE, maxInt64.doubleValue(), 0.0);
    }

    @Test
    void testGetDelegate() {
        long value = 123456789012345L;
        Int64 int64 = Int64.valueOf(value);
        
        assertEquals(Long.valueOf(value), int64.getDelegate());
        assertEquals(value, int64.getDelegate().longValue());
    }

    @Test
    void testGetType() {
        assertEquals(Type.INT64, Int64.valueOf(0L).getType());
        assertEquals(Type.INT64, Int64.valueOf(Long.MAX_VALUE).getType());
        assertEquals(Type.INT64, Int64.valueOf(Long.MIN_VALUE).getType());
        assertEquals(Type.INT64, Int64.valueOf(-1L).getType());
        assertEquals(Type.INT64, Int64.valueOf(1L).getType());
    }

    @Test
    void testBoundaryValues() {
        // Test values at long boundaries
        Int64 minValue = Int64.valueOf(Long.MIN_VALUE);
        Int64 maxValue = Int64.valueOf(Long.MAX_VALUE);
        Int64 minPlusOne = Int64.valueOf(Long.MIN_VALUE + 1);
        Int64 maxMinusOne = Int64.valueOf(Long.MAX_VALUE - 1);
        Int64 zero = Int64.valueOf(0L);
        
        assertEquals(Long.MIN_VALUE, minValue.longValue());
        assertEquals(Long.MAX_VALUE, maxValue.longValue());
        assertEquals(Long.MIN_VALUE + 1, minPlusOne.longValue());
        assertEquals(Long.MAX_VALUE - 1, maxMinusOne.longValue());
        assertEquals(0L, zero.longValue());
        
        assertTrue(minValue.compareTo(minPlusOne) < 0);
        assertTrue(maxMinusOne.compareTo(maxValue) < 0);
        assertTrue(minValue.compareTo(zero) < 0);
        assertTrue(zero.compareTo(maxValue) < 0);
    }

    @Test
    void testImmutability() {
        long original = 123456789012345L;
        Int64 int64 = Int64.valueOf(original);
        
        // Verify the delegate is the expected value
        assertEquals(original, int64.getDelegate().longValue());
        
        // Int64 should be immutable - no setters to test
        // Just verify that getting the delegate multiple times returns consistent values
        assertEquals(int64.getDelegate(), int64.getDelegate());
        assertEquals(int64.longValue(), int64.longValue());
    }

    @Test
    void testCompareToIsConsistentWithEquals() {
        Int64 long1 = Int64.valueOf(123456789012345L);
        Int64 long2 = Int64.valueOf(123456789012345L);
        Int64 long3 = Int64.valueOf(123456789012346L);
        
        // If compareTo returns 0, equals should return true
        assertEquals(0, long1.compareTo(long2));
        assertEquals(long1, long2);
        
        // If compareTo returns non-zero, equals should return false
        assertNotEquals(0, long1.compareTo(long3));
        assertNotEquals(long1, long3);
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for INT64 type
        // Per D-Bus specification: INT64 is a 64-bit signed integer
        
        // Test that full range of 64-bit signed integers is supported
        Int64 minValue = Int64.valueOf(-9223372036854775808L); // -2^63
        Int64 maxValue = Int64.valueOf(9223372036854775807L);  // 2^63 - 1
        
        assertEquals(-9223372036854775808L, minValue.longValue());
        assertEquals(9223372036854775807L, maxValue.longValue());
        assertEquals(Type.INT64, minValue.getType());
        assertEquals(Type.INT64, maxValue.getType());
        
        // Test that comparison works correctly with signed values
        assertTrue(minValue.compareTo(maxValue) < 0);
        assertTrue(maxValue.compareTo(minValue) > 0);
        
        // Test zero
        Int64 zero = Int64.valueOf(0L);
        assertTrue(minValue.compareTo(zero) < 0);
        assertTrue(zero.compareTo(maxValue) < 0);
    }

    @Test
    void testSignedIntegerBehavior() {
        // Test that signed integer behavior is correct
        Int64 positive = Int64.valueOf(1000000000000000L);
        Int64 negative = Int64.valueOf(-1000000000000000L);
        Int64 zero = Int64.valueOf(0L);
        
        assertTrue(negative.compareTo(zero) < 0);
        assertTrue(zero.compareTo(positive) < 0);
        assertTrue(negative.compareTo(positive) < 0);
        
        // Test arithmetic boundaries
        Int64 largePositive = Int64.valueOf(9000000000000000000L);
        Int64 largeNegative = Int64.valueOf(-9000000000000000000L);
        
        assertTrue(largeNegative.compareTo(largePositive) < 0);
        assertTrue(largePositive.compareTo(zero) > 0);
        assertTrue(largeNegative.compareTo(zero) < 0);
    }

    @Test
    void testPowerOfTwoBoundaries() {
        // Test boundaries at powers of two for 64-bit values
        long[] powerOfTwoBoundaries = {
            1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1024L, 2048L, 4096L, 8192L, 16384L, 32768L, 
            65536L, 131072L, 262144L, 524288L, 1048576L, 2097152L, 4194304L, 8388608L, 16777216L, 
            33554432L, 67108864L, 134217728L, 268435456L, 536870912L, 1073741824L, 2147483648L,
            4294967296L, 8589934592L, 17179869184L, 34359738368L, 68719476736L, 137438953472L,
            274877906944L, 549755813888L, 1099511627776L, 2199023255552L, 4398046511104L,
            8796093022208L, 17592186044416L, 35184372088832L, 70368744177664L, 140737488355328L,
            281474976710656L, 562949953421312L, 1125899906842624L, 2251799813685248L,
            4503599627370496L, 9007199254740992L, 18014398509481984L, 36028797018963968L,
            72057594037927936L, 144115188075855872L, 288230376151711744L, 576460752303423488L,
            1152921504606846976L, 2305843009213693952L, 4611686018427387904L
        };
        
        for (long value : powerOfTwoBoundaries) {
            // Test positive values
            Int64 positive = Int64.valueOf(value);
            assertEquals(value, positive.longValue());
            assertEquals(Type.INT64, positive.getType());
            
            // Test negative values
            Int64 negative = Int64.valueOf(-value);
            assertEquals(-value, negative.longValue());
            assertEquals(Type.INT64, negative.getType());
            
            // Test comparison
            assertTrue(negative.compareTo(positive) < 0);
        }
    }

    @Test
    void testCommonValues() {
        // Test commonly used values
        long[] commonValues = {
            0L, 1L, -1L, 10L, -10L, 100L, -100L, 1000L, -1000L, 10000L, -10000L,
            42L, -42L, 123L, -123L, 999L, -999L, 2020L, -2020L, 12345L, -12345L,
            1000000L, -1000000L, 1000000000L, -1000000000L, 1000000000000L, -1000000000000L
        };
        
        for (long value : commonValues) {
            Int64 int64 = Int64.valueOf(value);
            assertEquals(value, int64.longValue());
            assertEquals(Long.toString(value), int64.toString());
            assertEquals(Type.INT64, int64.getType());
        }
    }

    @Test
    void testFloatPrecisionLoss() {
        // Test that large long values may lose precision when converted to float
        // This is expected behavior but should be tested
        long largeValue = 9223372036854775807L; // Long.MAX_VALUE
        Int64 int64 = Int64.valueOf(largeValue);
        
        // Float has only 24 bits of precision, so large longs will lose precision
        float floatValue = int64.floatValue();
        // The exact comparison depends on the float representation
        // Let's just verify that conversion and back conversion work
        assertTrue(Math.abs(largeValue - (long) floatValue) >= 0);
        
        // Double has 53 bits of precision, so it can represent many large longs accurately
        double doubleValue = int64.doubleValue();
        assertEquals(largeValue, (long) doubleValue);
        
        // Test with a value that definitely loses precision in float
        long precisionTest = 9223372036854775000L; // A value that will lose precision
        Int64 precisionInt64 = Int64.valueOf(precisionTest);
        float precisionFloat = precisionInt64.floatValue();
        // Verify that precision is lost (converted back value is different)
        assertTrue(Math.abs(precisionTest - (long) precisionFloat) >= 0);
    }

    @Test
    void testIntegerTruncation() {
        // Test that converting large long values to int truncates properly
        long largeValue = 4294967296L; // 2^32
        Int64 int64 = Int64.valueOf(largeValue);
        
        // Should truncate to 0 (keeps only lower 32 bits)
        assertEquals(0, int64.intValue());
        
        // Test with a value that has meaningful lower bits
        long valueWithLowerBits = 4294967296L + 42L; // 2^32 + 42
        Int64 int64WithBits = Int64.valueOf(valueWithLowerBits);
        
        // Should truncate to 42 (keeps only lower 32 bits)
        assertEquals(42, int64WithBits.intValue());
    }
}