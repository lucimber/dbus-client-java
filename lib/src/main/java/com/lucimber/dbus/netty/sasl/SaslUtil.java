/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty.sasl;

final class SaslUtil {

    private static final char[] HEX_ARRAY_LOWER =
            "0123456789abcdef"
                    .toCharArray(); // D-Bus spec requires lowercase hex for DBUS_COOKIE_SHA1

    private SaslUtil() {} // Static utility class

    public static String hexEncode(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY_LOWER[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY_LOWER[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexDecode(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Hex string to decode cannot be null.");
        }
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException(
                    "Hex string must have an even number of characters: " + s);
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int firstDigit = Character.digit(s.charAt(i), 16);
            int secondDigit = Character.digit(s.charAt(i + 1), 16);
            if (firstDigit == -1 || secondDigit == -1) {
                throw new IllegalArgumentException("Invalid hex character in string: " + s);
            }
            data[i / 2] = (byte) ((firstDigit << 4) + secondDigit);
        }
        return data;
    }
}
