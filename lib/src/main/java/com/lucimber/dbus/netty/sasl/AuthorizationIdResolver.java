/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Secur32.EXTENDED_NAME_FORMAT;
import com.sun.jna.platform.win32.Secur32Util;
import com.sun.jna.platform.win32.Win32Exception;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility to determine the authorization identity for use with the EXTERNAL SASL mechanism. Both
 * UID (Unix) and SID (Windows) are supported by this resolver.
 */
public final class AuthorizationIdResolver {

    private AuthorizationIdResolver() {}

    /**
     * Resolves the current user's UID (Unix) or SID (Windows) as a string.
     *
     * @return The authorization identity string.
     * @throws SaslMechanismException if the ID cannot be resolved.
     */
    public static String resolve() throws SaslMechanismException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getWindowsSid();
        } else {
            return getUnixUid();
        }
    }

    private static String getWindowsSid() throws SaslMechanismException {
        try {
            // Preferred: DOMAIN\Username format
            String domainUser = Secur32Util.getUserNameEx(EXTENDED_NAME_FORMAT.NameSamCompatible);
            return Advapi32Util.getAccountByName(domainUser).sidString;
        } catch (Win32Exception e1) {
            try {
                // Fallback: simple username
                String user = Advapi32Util.getUserName();
                return Advapi32Util.getAccountByName(user).sidString;
            } catch (Win32Exception e2) {
                throw new SaslMechanismException(
                        "Failed to retrieve Windows SID from either domain or local user.", e2);
            }
        }
    }

    private static String getUnixUid() throws SaslMechanismException {
        try {
            // Use the standard Java NIO Files API to get UID from /proc/self
            // This is the recommended approach for 2024 and avoids internal APIs
            Object uidAttr = Files.getAttribute(Paths.get("/proc/self"), "unix:uid");
            return uidAttr.toString();
        } catch (IOException e) {
            throw new SaslMechanismException("Failed to retrieve Unix UID from /proc/self", e);
        }
    }
}
