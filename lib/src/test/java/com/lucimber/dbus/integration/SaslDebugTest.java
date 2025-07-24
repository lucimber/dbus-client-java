/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.integration;

import com.lucimber.dbus.netty.sasl.AuthorizationIdResolver;
import com.lucimber.dbus.netty.sasl.SaslMechanismException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Test class to debug SASL authorization ID resolution */
public class SaslDebugTest {
    public static void main(String[] args) {
        System.out.println("=== SASL Authorization ID Debug ===");

        // Check system properties
        System.out.println("user.name: " + System.getProperty("user.name"));
        System.out.println("os.name: " + System.getProperty("os.name"));

        // Check /proc/self
        System.out.println("/proc/self exists: " + Files.exists(Paths.get("/proc/self")));

        // Try to get UID using Files.getAttribute
        try {
            Object uidAttr = Files.getAttribute(Paths.get("/proc/self"), "unix:uid");
            System.out.println("Unix UID from /proc/self: " + uidAttr);
        } catch (IOException e) {
            System.out.println("Failed to get Unix UID from /proc/self: " + e.getMessage());
        }

        // Try AuthorizationIdResolver
        try {
            String authId = AuthorizationIdResolver.resolve();
            System.out.println("AuthorizationIdResolver result: " + authId);
        } catch (SaslMechanismException e) {
            System.out.println("AuthorizationIdResolver failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
