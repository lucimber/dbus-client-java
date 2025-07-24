/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty.sasl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SaslUtilTest {

    @Test
    void testHexEncodeNullBytes() {
        String result = SaslUtil.hexEncode(null);
        assertEquals("", result);
    }

    @Test
    void testHexEncodeEmptyBytes() {
        byte[] bytes = {};
        String result = SaslUtil.hexEncode(bytes);
        assertEquals("", result);
    }

    @Test
    void testHexEncodeSingleByte() {
        byte[] bytes = {0x0F};
        String result = SaslUtil.hexEncode(bytes);
        assertEquals("0f", result);
    }

    @Test
    void testHexEncodeMultipleBytes() {
        byte[] bytes = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        String result = SaslUtil.hexEncode(bytes);
        assertEquals("0123456789abcdef", result);
    }

    @Test
    void testHexEncodeAllByteValues() {
        byte[] bytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            bytes[i] = (byte) i;
        }
        String result = SaslUtil.hexEncode(bytes);

        // Verify the result starts and ends correctly
        assertTrue(result.startsWith("00"));
        assertTrue(result.endsWith("ff"));
        assertEquals(512, result.length()); // 256 bytes * 2 hex chars per byte
    }

    @Test
    void testHexDecodeNullString() {
        assertThrows(IllegalArgumentException.class, () -> SaslUtil.hexDecode(null));
    }

    @Test
    void testHexDecodeEmptyString() {
        byte[] result = SaslUtil.hexDecode("");
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void testHexDecodeValidString() {
        String hex = "0123456789abcdef";
        byte[] result = SaslUtil.hexDecode(hex);
        byte[] expected = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
        };
        assertArrayEquals(expected, result);
    }

    @Test
    void testHexDecodeUppercaseString() {
        String hex = "0123456789ABCDEF";
        byte[] result = SaslUtil.hexDecode(hex);
        byte[] expected = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
        };
        assertArrayEquals(expected, result);
    }

    @Test
    void testHexDecodeMixedCaseString() {
        String hex = "0123456789AbCdEf";
        byte[] result = SaslUtil.hexDecode(hex);
        byte[] expected = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
        };
        assertArrayEquals(expected, result);
    }

    @Test
    void testHexDecodeOddLengthString() {
        String hex = "123";
        assertThrows(IllegalArgumentException.class, () -> SaslUtil.hexDecode(hex));
    }

    @Test
    void testHexDecodeInvalidCharacter() {
        String hex = "12XY";
        assertThrows(IllegalArgumentException.class, () -> SaslUtil.hexDecode(hex));
    }

    @Test
    void testHexDecodeInvalidCharacterAtEnd() {
        String hex = "12G3";
        assertThrows(IllegalArgumentException.class, () -> SaslUtil.hexDecode(hex));
    }

    @Test
    void testHexDecodeWithSpaces() {
        String hex = "12 34";
        assertThrows(IllegalArgumentException.class, () -> SaslUtil.hexDecode(hex));
    }

    @Test
    void testHexEncodeDecodeRoundTrip() {
        byte[] original = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x0A, 0x0B, 0x0C};
        String encoded = SaslUtil.hexEncode(original);
        byte[] decoded = SaslUtil.hexDecode(encoded);

        assertArrayEquals(original, decoded);
    }

    @Test
    void testHexDecodeEncodeRoundTrip() {
        String original = "0123456789abcdef";
        byte[] decoded = SaslUtil.hexDecode(original);
        String encoded = SaslUtil.hexEncode(decoded);

        assertEquals(original, encoded);
    }

    @Test
    void testHexEncodeLowercaseOutput() {
        // Verify that hex encoding always produces lowercase output as required by D-Bus spec
        byte[] bytes = {(byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        String result = SaslUtil.hexEncode(bytes);
        assertEquals("abcdef", result);
        assertFalse(result.contains("A"));
        assertFalse(result.contains("B"));
        assertFalse(result.contains("C"));
        assertFalse(result.contains("D"));
        assertFalse(result.contains("E"));
        assertFalse(result.contains("F"));
    }
}
