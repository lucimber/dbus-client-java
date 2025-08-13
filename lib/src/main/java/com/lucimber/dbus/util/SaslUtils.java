/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

/** Utility class for common methods used for SASL authentication. */
public final class SaslUtils {

    private SaslUtils() {
        // Utility class
    }

    /**
     * Translates a byte-array to its corresponding hexadecimal representation.
     *
     * @param bytes The byte-array
     * @return The hexadecimal representation as a {@link String}.
     */
    public static String toHexadecimalString(final byte[] bytes) {
        Objects.requireNonNull(bytes);
        final BigInteger integer = new BigInteger(1, bytes);
        final int hexRadix = 16;
        return integer.toString(hexRadix);
    }

    /**
     * Translates a hexadecimal string to its corresponding binary representation.
     *
     * @param hexString The hexadecimal string
     * @return The binary representation as a byte-array.
     */
    public static byte[] fromHexadecimalString(final String hexString) {
        Objects.requireNonNull(hexString);
        final int hexRadix = 16;
        final BigInteger integer = new BigInteger(hexString, hexRadix);
        return integer.toByteArray();
    }

    /**
     * Generates an ASCII-safe challenge string.
     *
     * @param challengeLength The desired length of the challenge.
     * @return The challenge as a {@link String}.
     */
    public static String generateChallengeString(final int challengeLength) {
        if (challengeLength <= 0) {
            throw new IllegalArgumentException("length must be greater than zero");
        }
        final String symbols =
                "abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789";
        final int bound = symbols.length();
        final SecureRandom random = new SecureRandom();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < challengeLength; i++) {
            final int idx = random.nextInt(bound);
            builder.append(symbols.charAt(idx));
        }
        return builder.toString();
    }

    /**
     * Locates the cookie file for the DBUS_COOKIE_SHA1 authentication mechanism.
     *
     * @param directoryPath The path of the directory in which the file resides.
     * @param cookeFileName The name of the cookie file.
     * @return The resolved path of the cookie file.
     * @throws FileNotFoundException If the resolved path points to no (regular) file.
     * @throws AccessDeniedException If the cookie file cannot be accessed by the VM.
     */
    public static Path locateCookieFile(final Path directoryPath, final String cookeFileName)
            throws FileNotFoundException, AccessDeniedException {
        Objects.requireNonNull(directoryPath);
        Objects.requireNonNull(cookeFileName);
        if (cookeFileName.isEmpty()) {
            throw new IllegalArgumentException("file name must not be empty");
        }
        final Path resolvedPath = Paths.get(directoryPath.toString(), cookeFileName);
        if (Files.isRegularFile(resolvedPath)) {
            if (Files.isReadable(resolvedPath)) {
                return resolvedPath;
            } else {
                throw new AccessDeniedException(resolvedPath.toString());
            }
        } else {
            throw new FileNotFoundException(resolvedPath.toString());
        }
    }

    /**
     * Computes a hash value via the SHA.
     *
     * @param bytes The bytes that should be hashed.
     * @return The hash as a byte-array.
     * @throws NoSuchAlgorithmException If the JVM is not capable of computing the hash via SHA.
     */
    public static byte[] computeHashValue(final byte[] bytes) throws NoSuchAlgorithmException {
        Objects.requireNonNull(bytes);
        final MessageDigest digest = MessageDigest.getInstance("SHA");
        return digest.digest(bytes);
    }
}
