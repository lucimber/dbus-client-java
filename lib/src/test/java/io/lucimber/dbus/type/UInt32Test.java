package io.lucimber.dbus.type;

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
}