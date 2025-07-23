/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection.sasl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SaslAuthConfigTest {

    @Test
    void testSaslAnonymousAuthConfig() {
        SaslAnonymousAuthConfig config = new SaslAnonymousAuthConfig();
        
        assertEquals(SaslAuthMechanism.ANONYMOUS, config.getAuthMechanism());
    }

    @Test
    void testSaslExternalAuthConfig() {
        String identity = "1000";
        SaslExternalAuthConfig config = new SaslExternalAuthConfig(identity);
        
        assertEquals(SaslAuthMechanism.EXTERNAL, config.getAuthMechanism());
        assertEquals(identity, config.getIdentity());
    }

    @Test
    void testSaslCookieAuthConfig() {
        String identity = "testuser";
        String cookieDirPath = "/home/testuser/.dbus-keyrings";
        SaslCookieAuthConfig config = new SaslCookieAuthConfig(identity, cookieDirPath);
        
        assertEquals(SaslAuthMechanism.COOKIE, config.getAuthMechanism());
        assertEquals(identity, config.getIdentity());
        assertEquals(cookieDirPath, config.getAbsCookieDirPath());
    }

    @Test
    void testSaslAuthMechanismValues() {
        // Test all enum values are accessible
        assertEquals("ANONYMOUS", SaslAuthMechanism.ANONYMOUS.name());
        assertEquals("EXTERNAL", SaslAuthMechanism.EXTERNAL.name());
        assertEquals("COOKIE", SaslAuthMechanism.COOKIE.name());
        
        // Test valueOf
        assertEquals(SaslAuthMechanism.ANONYMOUS, SaslAuthMechanism.valueOf("ANONYMOUS"));
        assertEquals(SaslAuthMechanism.EXTERNAL, SaslAuthMechanism.valueOf("EXTERNAL"));
        assertEquals(SaslAuthMechanism.COOKIE, SaslAuthMechanism.valueOf("COOKIE"));
        
        // Test all values
        SaslAuthMechanism[] values = SaslAuthMechanism.values();
        assertEquals(3, values.length);
        assertTrue(containsMechanism(values, SaslAuthMechanism.ANONYMOUS));
        assertTrue(containsMechanism(values, SaslAuthMechanism.EXTERNAL));
        assertTrue(containsMechanism(values, SaslAuthMechanism.COOKIE));
    }

    @Test
    void testSaslAuthMechanismEnumMethods() {
        // Test ordinal values
        assertEquals(0, SaslAuthMechanism.EXTERNAL.ordinal());
        assertEquals(1, SaslAuthMechanism.COOKIE.ordinal());
        assertEquals(2, SaslAuthMechanism.ANONYMOUS.ordinal());
        
        // Test toString (returns the string representation for SASL)
        assertEquals("EXTERNAL", SaslAuthMechanism.EXTERNAL.toString());
        assertEquals("DBUS_COOKIE_SHA1", SaslAuthMechanism.COOKIE.toString());
        assertEquals("ANONYMOUS", SaslAuthMechanism.ANONYMOUS.toString());
    }

    @Test
    void testInvalidEnumValue() {
        assertThrows(IllegalArgumentException.class, () -> SaslAuthMechanism.valueOf("INVALID"));
    }

    private boolean containsMechanism(SaslAuthMechanism[] mechanisms, SaslAuthMechanism target) {
        for (SaslAuthMechanism mechanism : mechanisms) {
            if (mechanism == target) {
                return true;
            }
        }
        return false;
    }
}