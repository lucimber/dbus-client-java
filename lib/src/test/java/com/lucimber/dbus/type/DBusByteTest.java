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

final class DBusByteTest {

    @Test
    void createByteWithMinValue() {
        byte min = 0;
        DBusByte dbusByte = DBusByte.valueOf(min);

        assertEquals(0, dbusByte.intValue());
        assertEquals(Type.BYTE, dbusByte.getType());
        assertEquals("0", dbusByte.toString());
    }

    @Test
    void createByteWithMaxValue() {
        byte max = (byte) 255; // 0xFF, will be -1 when cast to signed byte
        DBusByte dbusByte = DBusByte.valueOf(max);

        assertEquals(255, dbusByte.intValue()); // Should return unsigned value
        assertEquals(Type.BYTE, dbusByte.getType());
        assertEquals("255", dbusByte.toString());
    }

    @Test
    void createByteWithMidValue() {
        byte mid = (byte) 128; // 0x80, will be -128 when cast to signed byte
        DBusByte dbusByte = DBusByte.valueOf(mid);

        assertEquals(128, dbusByte.intValue()); // Should return unsigned value
        assertEquals(Type.BYTE, dbusByte.getType());
        assertEquals("128", dbusByte.toString());
    }

    @Test
    void createByteWithTypicalValue() {
        byte typical = 123;
        DBusByte dbusByte = DBusByte.valueOf(typical);

        assertEquals(123, dbusByte.intValue());
        assertEquals(Type.BYTE, dbusByte.getType());
        assertEquals("123", dbusByte.toString());
    }

    @ParameterizedTest
    @ValueSource(bytes = {0, 1, 127, -128, -1, 42, 100, (byte) 200, (byte) 255})
    void createByteWithVariousValues(byte value) {
        DBusByte dbusByte = DBusByte.valueOf(value);

        assertEquals(Byte.toUnsignedInt(value), dbusByte.intValue());
        assertEquals(Type.BYTE, dbusByte.getType());
        assertEquals(Integer.toString(Byte.toUnsignedInt(value)), dbusByte.toString());
    }

    @Test
    void testSignedToUnsignedConversion() {
        // Test conversion from signed byte to unsigned representation
        DBusByte dbusByte = DBusByte.valueOf((byte) 0x80); // 128 in unsigned, -128 in signed

        assertEquals(128, dbusByte.intValue());
        assertEquals(-128, dbusByte.byteValue()); // When cast to signed byte
        assertEquals(128L, dbusByte.longValue());
        assertEquals("128", dbusByte.toString());
    }

    @Test
    void testMaxUnsignedValue() {
        DBusByte dbusByte = DBusByte.valueOf((byte) 0xFF); // 255 in unsigned, -1 in signed

        assertEquals(255, dbusByte.intValue());
        assertEquals(-1, dbusByte.byteValue()); // When cast to signed byte
        assertEquals(255L, dbusByte.longValue());
        assertEquals("255", dbusByte.toString());
    }

    @Test
    void testEquals() {
        DBusByte byte1 = DBusByte.valueOf((byte) 123);
        DBusByte byte2 = DBusByte.valueOf((byte) 123);
        DBusByte byte3 = DBusByte.valueOf((byte) 124);

        assertEquals(byte1, byte2);
        assertNotEquals(byte1, byte3);
        assertEquals(byte1, byte1); // self-equality

        assertNotEquals(byte1, null);
        assertNotEquals(byte1, "123"); // Different type
        assertNotEquals(byte1, (byte) 123); // Different type
    }

    @Test
    void testHashCode() {
        DBusByte byte1 = DBusByte.valueOf((byte) 123);
        DBusByte byte2 = DBusByte.valueOf((byte) 123);
        DBusByte byte3 = DBusByte.valueOf((byte) 124);

        assertEquals(byte1.hashCode(), byte2.hashCode());
        assertNotEquals(byte1.hashCode(), byte3.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("0", DBusByte.valueOf((byte) 0).toString());
        assertEquals("123", DBusByte.valueOf((byte) 123).toString());
        assertEquals("255", DBusByte.valueOf((byte) 255).toString());
        assertEquals("128", DBusByte.valueOf((byte) 128).toString());
    }

    @Test
    void testCompareTo() {
        DBusByte small = DBusByte.valueOf((byte) 10);
        DBusByte medium = DBusByte.valueOf((byte) 20);
        DBusByte large = DBusByte.valueOf((byte) 30);
        DBusByte duplicate = DBusByte.valueOf((byte) 20);

        assertTrue(small.compareTo(medium) < 0);
        assertTrue(medium.compareTo(large) < 0);
        assertTrue(large.compareTo(medium) > 0);
        assertTrue(medium.compareTo(small) > 0);
        assertEquals(0, medium.compareTo(duplicate));
        assertEquals(0, medium.compareTo(medium));
    }

    @Test
    void testCompareToWithExtremeValues() {
        DBusByte min = DBusByte.valueOf((byte) 0);
        DBusByte max = DBusByte.valueOf((byte) 255);
        DBusByte mid = DBusByte.valueOf((byte) 128);

        assertTrue(min.compareTo(max) < 0);
        assertTrue(max.compareTo(min) > 0);
        assertTrue(min.compareTo(mid) < 0);
        assertTrue(mid.compareTo(min) > 0);
        assertTrue(mid.compareTo(max) < 0);
        assertTrue(max.compareTo(mid) > 0);
    }

    @Test
    void testNumberMethods() {
        DBusByte dbusByte = DBusByte.valueOf((byte) 123);

        assertEquals(123, dbusByte.intValue());
        assertEquals(123L, dbusByte.longValue());
        assertEquals(123.0f, dbusByte.floatValue(), 0.0f);
        assertEquals(123.0, dbusByte.doubleValue(), 0.0);
        assertEquals((byte) 123, dbusByte.byteValue());
        assertEquals((short) 123, dbusByte.shortValue());
    }

    @Test
    void testNumberMethodsWithLargeValues() {
        // Test with value that's positive in unsigned but negative in signed
        DBusByte dbusByte = DBusByte.valueOf((byte) 200); // Larger than Byte.MAX_VALUE

        assertEquals(200, dbusByte.intValue());
        assertEquals(200L, dbusByte.longValue());
        assertEquals(200.0f, dbusByte.floatValue(), 0.0f);
        assertEquals(200.0, dbusByte.doubleValue(), 0.0);
        assertEquals((byte) 200, dbusByte.byteValue()); // This will be negative when cast to byte
    }

    @Test
    void testNumberMethodsWithExtremeValues() {
        // Test with MIN_VALUE (0)
        DBusByte min = DBusByte.valueOf((byte) 0);
        assertEquals(0, min.intValue());
        assertEquals(0L, min.longValue());
        assertEquals(0.0f, min.floatValue(), 0.0f);
        assertEquals(0.0, min.doubleValue(), 0.0);

        // Test with MAX_VALUE (255)
        DBusByte max = DBusByte.valueOf((byte) 255);
        assertEquals(255, max.intValue());
        assertEquals(255L, max.longValue());
        assertEquals(255.0f, max.floatValue(), 0.0f);
        assertEquals(255.0, max.doubleValue(), 0.0);
    }

    @Test
    void testGetDelegate() {
        byte value = 123;
        DBusByte dbusByte = DBusByte.valueOf(value);

        assertEquals(Byte.valueOf(value), dbusByte.getDelegate());
        assertEquals(value, dbusByte.getDelegate().byteValue());
    }

    @Test
    void testGetType() {
        assertEquals(Type.BYTE, DBusByte.valueOf((byte) 0).getType());
        assertEquals(Type.BYTE, DBusByte.valueOf((byte) 255).getType());
        assertEquals(Type.BYTE, DBusByte.valueOf((byte) 128).getType());
    }

    @Test
    void testBoundaryValues() {
        // Test values at the boundary
        DBusByte minValue = DBusByte.valueOf((byte) 0);
        DBusByte maxValue = DBusByte.valueOf((byte) 255);
        DBusByte minPlusOne = DBusByte.valueOf((byte) 1);
        DBusByte maxMinusOne = DBusByte.valueOf((byte) 254);
        DBusByte signedBoundary =
                DBusByte.valueOf((byte) 128); // Where signed byte becomes negative

        assertEquals(0, minValue.intValue());
        assertEquals(255, maxValue.intValue());
        assertEquals(1, minPlusOne.intValue());
        assertEquals(254, maxMinusOne.intValue());
        assertEquals(128, signedBoundary.intValue());

        assertTrue(minValue.compareTo(minPlusOne) < 0);
        assertTrue(maxMinusOne.compareTo(maxValue) < 0);
        assertTrue(minValue.compareTo(signedBoundary) < 0);
        assertTrue(signedBoundary.compareTo(maxValue) < 0);
    }

    @Test
    void testSignedByteBoundaryBehavior() {
        // Test behavior at the signed byte boundary
        DBusByte maxSigned = DBusByte.valueOf((byte) 127); // Byte.MAX_VALUE
        DBusByte minUnsigned = DBusByte.valueOf((byte) 128); // Where byte becomes negative

        assertEquals(127, maxSigned.intValue());
        assertEquals(127, maxSigned.byteValue());

        assertEquals(128, minUnsigned.intValue());
        assertEquals(-128, minUnsigned.byteValue()); // Wraps to negative

        assertTrue(maxSigned.compareTo(minUnsigned) < 0); // 127 < 128 in unsigned comparison
    }

    @Test
    void testImmutability() {
        byte original = 123;
        DBusByte dbusByte = DBusByte.valueOf(original);

        // Verify the delegate is the expected value
        assertEquals(original, dbusByte.getDelegate().byteValue());

        // DBusByte should be immutable - no setters to test
        // Just verify that getting the delegate multiple times returns consistent values
        assertEquals(dbusByte.getDelegate(), dbusByte.getDelegate());
        assertEquals(dbusByte.intValue(), dbusByte.intValue());
    }

    @Test
    void testCompareToIsConsistentWithEquals() {
        DBusByte byte1 = DBusByte.valueOf((byte) 123);
        DBusByte byte2 = DBusByte.valueOf((byte) 123);
        DBusByte byte3 = DBusByte.valueOf((byte) 124);

        // If compareTo returns 0, equals should return true
        assertEquals(0, byte1.compareTo(byte2));
        assertEquals(byte1, byte2);

        // If compareTo returns non-zero, equals should return false
        assertNotEquals(0, byte1.compareTo(byte3));
        assertNotEquals(byte1, byte3);
    }

    @Test
    void testUnsignedComparisonLogic() {
        // Test that comparison works correctly for unsigned values
        DBusByte large1 = DBusByte.valueOf((byte) 200); // > Byte.MAX_VALUE
        DBusByte large2 = DBusByte.valueOf((byte) 250); // > Byte.MAX_VALUE
        DBusByte small = DBusByte.valueOf((byte) 50); // < Byte.MAX_VALUE

        assertTrue(small.compareTo(large1) < 0);
        assertTrue(large1.compareTo(large2) < 0);
        assertTrue(large2.compareTo(small) > 0);
        assertTrue(large2.compareTo(large1) > 0);
    }

    @Test
    void testDBusSpecificationCompliance() {
        // Test D-Bus specification compliance for BYTE type
        // Per D-Bus specification: BYTE is an unsigned 8-bit integer (0-255)

        // Test minimum value
        DBusByte minByte = DBusByte.valueOf((byte) 0);
        assertEquals(0, minByte.intValue());
        assertEquals(Type.BYTE, minByte.getType());

        // Test maximum value
        DBusByte maxByte = DBusByte.valueOf((byte) 255);
        assertEquals(255, maxByte.intValue());
        assertEquals(Type.BYTE, maxByte.getType());

        // Test that all values in the range are valid
        for (int i = 0; i <= 255; i++) {
            byte value = (byte) i;
            final int expectedValue = i;
            assertDoesNotThrow(
                    () -> {
                        DBusByte dbusByte = DBusByte.valueOf(value);
                        assertEquals(expectedValue, dbusByte.intValue());
                        assertEquals(Type.BYTE, dbusByte.getType());
                    });
        }
    }

    @Test
    void testAllPossibleByteValues() {
        // Test all possible byte values (0-255 in unsigned representation)
        for (int i = 0; i <= 255; i++) {
            byte value = (byte) i;
            DBusByte dbusByte = DBusByte.valueOf(value);

            assertEquals(i, dbusByte.intValue());
            assertEquals(i, dbusByte.longValue());
            assertEquals(Type.BYTE, dbusByte.getType());
            assertEquals(Integer.toString(i), dbusByte.toString());
        }
    }
}
