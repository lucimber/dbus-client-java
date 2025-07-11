/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

final class UnixFdTest {

    @Test
    void createUnixFdWithZero() {
        UnixFd unixFd = UnixFd.valueOf(0);
        
        assertEquals(0, unixFd.getDelegate().intValue());
        assertEquals(Type.UNIX_FD, unixFd.getType());
        assertEquals("0", unixFd.toString());
    }

    @Test
    void createUnixFdWithPositiveValue() {
        int value = 42;
        UnixFd unixFd = UnixFd.valueOf(value);
        
        assertEquals(value, unixFd.getDelegate().intValue());
        assertEquals(Type.UNIX_FD, unixFd.getType());
        assertEquals(Integer.toUnsignedString(value), unixFd.toString());
    }

    @Test
    void createUnixFdWithLargeUnsignedValue() {
        // Test with a value that would be negative in signed representation
        int value = -1; // Represents 4294967295 in unsigned
        UnixFd unixFd = UnixFd.valueOf(value);
        
        assertEquals(value, unixFd.getDelegate().intValue());
        assertEquals(Type.UNIX_FD, unixFd.getType());
        assertEquals("4294967295", unixFd.toString());
    }

    @Test
    void createUnixFdWithCommonFdValues() {
        // Test with common file descriptor values
        int[] commonFds = {0, 1, 2, 3, 4, 5, 10, 20, 100, 255, 256, 1024};
        
        for (int fd : commonFds) {
            UnixFd unixFd = UnixFd.valueOf(fd);
            assertEquals(fd, unixFd.getDelegate().intValue());
            assertEquals(Type.UNIX_FD, unixFd.getType());
            assertEquals(Integer.toUnsignedString(fd), unixFd.toString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 42, 100, 255, 256, 1024, 65535, 65536, 
                        Integer.MAX_VALUE, -1, -100, Integer.MIN_VALUE})
    void createUnixFdWithVariousValues(int value) {
        UnixFd unixFd = UnixFd.valueOf(value);
        
        assertEquals(value, unixFd.getDelegate().intValue());
        assertEquals(Type.UNIX_FD, unixFd.getType());
        assertEquals(Integer.toUnsignedString(value), unixFd.toString());
    }

    @Test
    void testEquals() {
        UnixFd fd1 = UnixFd.valueOf(42);
        UnixFd fd2 = UnixFd.valueOf(42);
        UnixFd fd3 = UnixFd.valueOf(43);
        
        assertEquals(fd1, fd2);
        assertNotEquals(fd1, fd3);
        assertEquals(fd1, fd1); // self-equality
        
        assertNotEquals(fd1, null);
        assertNotEquals(fd1, "42"); // Different type
        assertNotEquals(fd1, 42); // Different type
    }

    @Test
    void testEqualsWithUnsignedValues() {
        UnixFd fd1 = UnixFd.valueOf(-1); // Max unsigned value
        UnixFd fd2 = UnixFd.valueOf(-1);
        UnixFd fd3 = UnixFd.valueOf(-2);
        
        assertEquals(fd1, fd2);
        assertNotEquals(fd1, fd3);
    }

    @Test
    void testHashCode() {
        UnixFd fd1 = UnixFd.valueOf(42);
        UnixFd fd2 = UnixFd.valueOf(42);
        UnixFd fd3 = UnixFd.valueOf(43);
        
        assertEquals(fd1.hashCode(), fd2.hashCode());
        assertNotEquals(fd1.hashCode(), fd3.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("0", UnixFd.valueOf(0).toString());
        assertEquals("1", UnixFd.valueOf(1).toString());
        assertEquals("2", UnixFd.valueOf(2).toString());
        assertEquals("42", UnixFd.valueOf(42).toString());
        assertEquals("2147483647", UnixFd.valueOf(Integer.MAX_VALUE).toString());
        assertEquals("4294967295", UnixFd.valueOf(-1).toString()); // Max unsigned
        assertEquals("2147483648", UnixFd.valueOf(Integer.MIN_VALUE).toString()); // 2^31 unsigned
    }

    @Test
    void testGetDelegate() {
        int value = 42;
        UnixFd unixFd = UnixFd.valueOf(value);
        
        assertEquals(Integer.valueOf(value), unixFd.getDelegate());
        assertEquals(value, unixFd.getDelegate().intValue());
    }

    @Test
    void testGetType() {
        assertEquals(Type.UNIX_FD, UnixFd.valueOf(0).getType());
        assertEquals(Type.UNIX_FD, UnixFd.valueOf(1).getType());
        assertEquals(Type.UNIX_FD, UnixFd.valueOf(2).getType());
        assertEquals(Type.UNIX_FD, UnixFd.valueOf(Integer.MAX_VALUE).getType());
        assertEquals(Type.UNIX_FD, UnixFd.valueOf(Integer.MIN_VALUE).getType());
        assertEquals(Type.UNIX_FD, UnixFd.valueOf(-1).getType());
    }

    @Test
    void testStandardFileDescriptors() {
        // Test standard file descriptors
        UnixFd stdin = UnixFd.valueOf(0);
        UnixFd stdout = UnixFd.valueOf(1);
        UnixFd stderr = UnixFd.valueOf(2);
        
        assertEquals(0, stdin.getDelegate().intValue());
        assertEquals(1, stdout.getDelegate().intValue());
        assertEquals(2, stderr.getDelegate().intValue());
        
        assertEquals("0", stdin.toString());
        assertEquals("1", stdout.toString());
        assertEquals("2", stderr.toString());
        
        assertEquals(Type.UNIX_FD, stdin.getType());
        assertEquals(Type.UNIX_FD, stdout.getType());
        assertEquals(Type.UNIX_FD, stderr.getType());
    }

    @Test
    void testBoundaryValues() {
        // Test values at unsigned 32-bit boundaries
        UnixFd zero = UnixFd.valueOf(0);
        UnixFd one = UnixFd.valueOf(1);
        UnixFd signedMax = UnixFd.valueOf(Integer.MAX_VALUE);
        UnixFd signedMaxPlusOne = UnixFd.valueOf(Integer.MIN_VALUE); // 2^31
        UnixFd unsignedMaxMinusOne = UnixFd.valueOf(-2);
        UnixFd unsignedMax = UnixFd.valueOf(-1);
        
        assertEquals(0, zero.getDelegate().intValue());
        assertEquals(1, one.getDelegate().intValue());
        assertEquals(Integer.MAX_VALUE, signedMax.getDelegate().intValue());
        assertEquals(Integer.MIN_VALUE, signedMaxPlusOne.getDelegate().intValue());
        assertEquals(-2, unsignedMaxMinusOne.getDelegate().intValue());
        assertEquals(-1, unsignedMax.getDelegate().intValue());
        
        assertEquals("0", zero.toString());
        assertEquals("1", one.toString());
        assertEquals("2147483647", signedMax.toString());
        assertEquals("2147483648", signedMaxPlusOne.toString());
        assertEquals("4294967294", unsignedMaxMinusOne.toString());
        assertEquals("4294967295", unsignedMax.toString());
    }

    @Test
    void testImmutability() {
        int original = 42;
        UnixFd unixFd = UnixFd.valueOf(original);
        
        // Verify the delegate is the expected value
        assertEquals(original, unixFd.getDelegate().intValue());
        
        // UnixFd should be immutable - no setters to test
        // Just verify that getting the delegate multiple times returns consistent values
        assertEquals(unixFd.getDelegate(), unixFd.getDelegate());
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for UNIX_FD type
        // Per D-Bus specification: UNIX_FD is a 32-bit unsigned integer representing a file descriptor
        
        // Test that full range of 32-bit unsigned integers is supported
        UnixFd minValue = UnixFd.valueOf(0);
        UnixFd maxValue = UnixFd.valueOf(-1); // 4294967295 (2^32 - 1)
        
        assertEquals(0, minValue.getDelegate().intValue());
        assertEquals(-1, maxValue.getDelegate().intValue()); // Raw signed representation
        assertEquals("0", minValue.toString());
        assertEquals("4294967295", maxValue.toString());
        assertEquals(Type.UNIX_FD, minValue.getType());
        assertEquals(Type.UNIX_FD, maxValue.getType());
        
        // Test common file descriptor values
        UnixFd[] commonFds = {
            UnixFd.valueOf(0),   // stdin
            UnixFd.valueOf(1),   // stdout
            UnixFd.valueOf(2),   // stderr
            UnixFd.valueOf(3),   // first user fd
            UnixFd.valueOf(255), // common upper limit for some systems
            UnixFd.valueOf(1024) // common ulimit value
        };
        
        for (UnixFd fd : commonFds) {
            assertEquals(Type.UNIX_FD, fd.getType());
            assertNotNull(fd.toString());
            assertNotNull(fd.getDelegate());
        }
    }

    @Test
    void testUnsignedIntegerBehavior() {
        // Test that unsigned integer behavior is correct for toString()
        UnixFd zero = UnixFd.valueOf(0);
        UnixFd small = UnixFd.valueOf(1000);
        UnixFd large = UnixFd.valueOf(Integer.MAX_VALUE);
        UnixFd veryLarge = UnixFd.valueOf(Integer.MIN_VALUE); // 2^31 unsigned
        UnixFd maximum = UnixFd.valueOf(-1); // Max unsigned
        
        // Test string representations
        assertEquals("0", zero.toString());
        assertEquals("1000", small.toString());
        assertEquals("2147483647", large.toString());
        assertEquals("2147483648", veryLarge.toString());
        assertEquals("4294967295", maximum.toString());
        
        // Test that they all have the correct type
        assertEquals(Type.UNIX_FD, zero.getType());
        assertEquals(Type.UNIX_FD, small.getType());
        assertEquals(Type.UNIX_FD, large.getType());
        assertEquals(Type.UNIX_FD, veryLarge.getType());
        assertEquals(Type.UNIX_FD, maximum.getType());
    }

    @Test
    void testPowerOfTwoBoundaries() {
        // Test boundaries at powers of two for 32-bit unsigned values
        int[] powerOfTwoBoundaries = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 
            65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 
            33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, Integer.MIN_VALUE // 2^31
        };
        
        for (int value : powerOfTwoBoundaries) {
            UnixFd unixFd = UnixFd.valueOf(value);
            assertEquals(value, unixFd.getDelegate().intValue());
            assertEquals(Type.UNIX_FD, unixFd.getType());
            assertEquals(Integer.toUnsignedString(value), unixFd.toString());
        }
    }

    @Test
    void testCommonValues() {
        // Test commonly used file descriptor values
        int[] commonValues = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            50, 100, 200, 255, 256, 511, 512, 1023, 1024, 2047, 2048, 4095, 4096,
            8191, 8192, 16383, 16384, 32767, 32768, 65535, 65536
        };
        
        for (int value : commonValues) {
            UnixFd unixFd = UnixFd.valueOf(value);
            assertEquals(value, unixFd.getDelegate().intValue());
            assertEquals(Integer.toUnsignedString(value), unixFd.toString());
            assertEquals(Type.UNIX_FD, unixFd.getType());
        }
    }

    @Test
    void testNegativeValuesAsUnsigned() {
        // Test that negative values in signed representation are handled as unsigned
        int[] negativeValues = {
            -1, -2, -3, -10, -100, -1000, -10000, -100000, -1000000, 
            Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 1000
        };
        
        for (int value : negativeValues) {
            UnixFd unixFd = UnixFd.valueOf(value);
            assertEquals(value, unixFd.getDelegate().intValue());
            assertEquals(Integer.toUnsignedString(value), unixFd.toString());
            assertEquals(Type.UNIX_FD, unixFd.getType());
            
            // Verify that toString produces unsigned representation
            assertFalse(unixFd.toString().startsWith("-"));
        }
    }

    @Test
    void testFileDescriptorRangeCompliance() {
        // Test that Unix file descriptors are properly represented
        // In Unix systems, file descriptors are typically non-negative integers
        // but D-Bus UNIX_FD type is unsigned 32-bit, so all values are valid
        
        // Test typical file descriptor range (0-1023 is common on many systems)
        for (int fd = 0; fd <= 1023; fd++) {
            UnixFd unixFd = UnixFd.valueOf(fd);
            assertEquals(fd, unixFd.getDelegate().intValue());
            assertEquals(Integer.toString(fd), unixFd.toString());
            assertEquals(Type.UNIX_FD, unixFd.getType());
        }
        
        // Test that large values are still valid (though may not be practical)
        UnixFd largeFd = UnixFd.valueOf(1000000);
        assertEquals(1000000, largeFd.getDelegate().intValue());
        assertEquals("1000000", largeFd.toString());
        assertEquals(Type.UNIX_FD, largeFd.getType());
    }

    @Test
    void testHashCodeConsistency() {
        // Test that hash codes are consistent with equals
        UnixFd fd1 = UnixFd.valueOf(42);
        UnixFd fd2 = UnixFd.valueOf(42);
        UnixFd fd3 = UnixFd.valueOf(43);
        
        assertEquals(fd1.hashCode(), fd2.hashCode());
        assertNotEquals(fd1.hashCode(), fd3.hashCode());
        
        // Test with unsigned values
        UnixFd maxFd1 = UnixFd.valueOf(-1);
        UnixFd maxFd2 = UnixFd.valueOf(-1);
        UnixFd almostMaxFd = UnixFd.valueOf(-2);
        
        assertEquals(maxFd1.hashCode(), maxFd2.hashCode());
        assertNotEquals(maxFd1.hashCode(), almostMaxFd.hashCode());
    }

    @Test
    void testToStringConsistency() {
        // Test that toString is consistent with Integer.toUnsignedString
        int[] testValues = {
            0, 1, 2, 42, 100, 255, 256, 1024, 65535, 65536, 
            Integer.MAX_VALUE, Integer.MIN_VALUE, -1, -2, -100, -1000
        };
        
        for (int value : testValues) {
            UnixFd unixFd = UnixFd.valueOf(value);
            String expected = Integer.toUnsignedString(value);
            assertEquals(expected, unixFd.toString());
        }
    }
}