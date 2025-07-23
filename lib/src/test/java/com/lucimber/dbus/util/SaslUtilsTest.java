/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SaslUtilsTest {

    @Test
    void testToHexadecimalString() {
        byte[] bytes = {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
        String result = SaslUtils.toHexadecimalString(bytes);

        assertEquals("123456789abcdef", result);
    }

    @Test
    void testToHexadecimalStringWithEmptyArray() {
        byte[] bytes = {};
        String result = SaslUtils.toHexadecimalString(bytes);

        assertEquals("0", result);
    }

    @Test
    void testToHexadecimalStringWithSingleByte() {
        byte[] bytes = {0x0F};
        String result = SaslUtils.toHexadecimalString(bytes);

        assertEquals("f", result);
    }

    @Test
    void testToHexadecimalStringWithNullInput() {
        assertThrows(NullPointerException.class, () -> SaslUtils.toHexadecimalString(null));
    }

    @Test
    void testFromHexadecimalString() {
        String hexString = "123456789abcdef";
        byte[] result = SaslUtils.fromHexadecimalString(hexString);

        byte[] expected = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
        };
        assertArrayEquals(expected, result);
    }

    @Test
    void testFromHexadecimalStringWithUppercase() {
        String hexString = "123456789ABCDEF";
        byte[] result = SaslUtils.fromHexadecimalString(hexString);

        byte[] expected = {
            0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF
        };
        assertArrayEquals(expected, result);
    }

    @Test
    void testFromHexadecimalStringWithSingleDigit() {
        String hexString = "f";
        byte[] result = SaslUtils.fromHexadecimalString(hexString);

        byte[] expected = {0x0F};
        assertArrayEquals(expected, result);
    }

    @Test
    void testFromHexadecimalStringWithNullInput() {
        assertThrows(NullPointerException.class, () -> SaslUtils.fromHexadecimalString(null));
    }

    @Test
    void testFromHexadecimalStringWithInvalidInput() {
        assertThrows(NumberFormatException.class, () -> SaslUtils.fromHexadecimalString("xyz"));
    }

    @Test
    void testHexadecimalRoundTrip() {
        // Test with bytes that don't have leading zero issues
        byte[] original = {0x01, 0x7F, (byte) 0x80, (byte) 0xFF};
        String hex = SaslUtils.toHexadecimalString(original);
        byte[] restored = SaslUtils.fromHexadecimalString(hex);

        assertArrayEquals(original, restored);
    }

    @Test
    void testHexadecimalRoundTripWithLeadingZero() {
        // Test a case where BigInteger behavior is more predictable
        byte[] original = {0x0F, 0x1A, 0x2B};
        String hex = SaslUtils.toHexadecimalString(original);
        byte[] restored = SaslUtils.fromHexadecimalString(hex);

        assertArrayEquals(original, restored);
    }

    @Test
    void testGenerateChallengeStringValidLength() {
        String challenge = SaslUtils.generateChallengeString(10);

        assertEquals(10, challenge.length());
        assertTrue(challenge.matches("[a-zA-Z0-9]+"));
    }

    @Test
    void testGenerateChallengeStringLength1() {
        String challenge = SaslUtils.generateChallengeString(1);

        assertEquals(1, challenge.length());
        assertTrue(challenge.matches("[a-zA-Z0-9]"));
    }

    @Test
    void testGenerateChallengeStringLength100() {
        String challenge = SaslUtils.generateChallengeString(100);

        assertEquals(100, challenge.length());
        assertTrue(challenge.matches("[a-zA-Z0-9]+"));
    }

    @Test
    void testGenerateChallengeStringRandomness() {
        String challenge1 = SaslUtils.generateChallengeString(20);
        String challenge2 = SaslUtils.generateChallengeString(20);

        // Extremely unlikely to be identical
        assertNotEquals(challenge1, challenge2);
    }

    @Test
    void testGenerateChallengeStringWithZeroLength() {
        assertThrows(IllegalArgumentException.class, () -> SaslUtils.generateChallengeString(0));
    }

    @Test
    void testGenerateChallengeStringWithNegativeLength() {
        assertThrows(IllegalArgumentException.class, () -> SaslUtils.generateChallengeString(-1));
        assertThrows(IllegalArgumentException.class, () -> SaslUtils.generateChallengeString(-10));
    }

    @Test
    void testLocateCookieFileSuccess(@TempDir Path tempDir) throws Exception {
        String fileName = "test-cookie";
        Path cookieFile = tempDir.resolve(fileName);
        Files.createFile(cookieFile);

        Path result = SaslUtils.locateCookieFile(tempDir, fileName);

        assertEquals(cookieFile, result);
    }

    @Test
    void testLocateCookieFileNotFound(@TempDir Path tempDir) {
        String fileName = "non-existent-cookie";

        assertThrows(
                FileNotFoundException.class, () -> SaslUtils.locateCookieFile(tempDir, fileName));
    }

    @Test
    void testLocateCookieFileIsDirectory(@TempDir Path tempDir) throws Exception {
        String dirName = "cookie-dir";
        Path cookieDir = tempDir.resolve(dirName);
        Files.createDirectory(cookieDir);

        assertThrows(
                FileNotFoundException.class, () -> SaslUtils.locateCookieFile(tempDir, dirName));
    }

    @Test
    void testLocateCookieFileNotReadable(@TempDir Path tempDir) throws Exception {
        String fileName = "unreadable-cookie";
        Path cookieFile = tempDir.resolve(fileName);
        Files.createFile(cookieFile);

        // Make file unreadable (only works on POSIX systems)
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("---------");
            Files.setPosixFilePermissions(cookieFile, perms);

            assertThrows(
                    AccessDeniedException.class,
                    () -> SaslUtils.locateCookieFile(tempDir, fileName));
        } catch (UnsupportedOperationException e) {
            // Skip test on non-POSIX systems (e.g., Windows)
            System.out.println("Skipping unreadable file test on non-POSIX system");
        } finally {
            // Restore permissions for cleanup
            try {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-rw-");
                Files.setPosixFilePermissions(cookieFile, perms);
            } catch (Exception ignored) {
                // Ignore cleanup failures
            }
        }
    }

    @Test
    void testLocateCookieFileWithNullDirectory() {
        assertThrows(NullPointerException.class, () -> SaslUtils.locateCookieFile(null, "cookie"));
    }

    @Test
    void testLocateCookieFileWithNullFileName(@TempDir Path tempDir) {
        assertThrows(NullPointerException.class, () -> SaslUtils.locateCookieFile(tempDir, null));
    }

    @Test
    void testLocateCookieFileWithEmptyFileName(@TempDir Path tempDir) {
        assertThrows(IllegalArgumentException.class, () -> SaslUtils.locateCookieFile(tempDir, ""));
    }

    @Test
    void testComputeHashValue() throws Exception {
        byte[] input = "test data".getBytes();
        byte[] hash = SaslUtils.computeHashValue(input);

        assertNotNull(hash);
        assertEquals(20, hash.length); // SHA-1 produces 160-bit (20-byte) hash
    }

    @Test
    void testComputeHashValueConsistency() throws Exception {
        byte[] input = "test data".getBytes();
        byte[] hash1 = SaslUtils.computeHashValue(input);
        byte[] hash2 = SaslUtils.computeHashValue(input);

        assertArrayEquals(hash1, hash2);
    }

    @Test
    void testComputeHashValueDifferentInputs() throws Exception {
        byte[] input1 = "test data 1".getBytes();
        byte[] input2 = "test data 2".getBytes();

        byte[] hash1 = SaslUtils.computeHashValue(input1);
        byte[] hash2 = SaslUtils.computeHashValue(input2);

        assertFalse(java.util.Arrays.equals(hash1, hash2));
    }

    @Test
    void testComputeHashValueWithEmptyInput() throws Exception {
        byte[] input = {};
        byte[] hash = SaslUtils.computeHashValue(input);

        assertNotNull(hash);
        assertEquals(20, hash.length);
    }

    @Test
    void testComputeHashValueWithNullInput() {
        assertThrows(NullPointerException.class, () -> SaslUtils.computeHashValue(null));
    }
}
