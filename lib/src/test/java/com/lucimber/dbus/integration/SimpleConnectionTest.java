/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.netty.NettyConnection;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple connection test to debug D-Bus connection issues */
public class SimpleConnectionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleConnectionTest.class);

    public static void main(String[] args) {
        LOGGER.info("=== Simple D-Bus Connection Test ===");

        // Enable detailed debugging via system properties for fallback
        System.setProperty("org.slf4j.simpleLogger.log.com.lucimber.dbus", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.io.netty", "debug");
        System.setProperty("org.slf4j.simpleLogger.log.root", "debug");

        // Log environment information
        LOGGER.info("Test environment:");
        LOGGER.info("  - Java version: {}", System.getProperty("java.version"));
        LOGGER.info("  - File encoding: {}", System.getProperty("file.encoding"));
        LOGGER.info("  - DBUS_SESSION_BUS_ADDRESS: {}", System.getenv("DBUS_SESSION_BUS_ADDRESS"));
        LOGGER.info(
                "  - Container mode: {}", new java.io.File("/.dockerenv").exists() ? "YES" : "NO");

        try {
            // Create configuration with shorter timeout for faster feedback
            ConnectionConfig config =
                    ConnectionConfig.builder().withConnectTimeout(Duration.ofSeconds(10)).build();

            LOGGER.info("Creating NettyConnection...");
            NettyConnection connection =
                    new NettyConnection(new InetSocketAddress("127.0.0.1", 12345), config);

            LOGGER.info("Starting connection...");
            long startTime = System.currentTimeMillis();

            CompletableFuture<Void> connectFuture = connection.connect().toCompletableFuture();

            try {
                connectFuture.get(15, TimeUnit.SECONDS);
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.info("Connection successful after {}ms", duration);
                LOGGER.info("Connection state: {}", connection.getState());
                LOGGER.info("Is connected: {}", connection.isConnected());

                // Clean up
                connection.close();

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                LOGGER.error("Connection failed after {}ms", duration);
                LOGGER.error("Exception: {}: {}", e.getClass().getSimpleName(), e.getMessage());
                if (e.getCause() != null) {
                    LOGGER.error(
                            "Caused by: {}: {}",
                            e.getCause().getClass().getSimpleName(),
                            e.getCause().getMessage());
                }

                // Print stack trace for debugging
                LOGGER.error("Full stack trace:", e);
                e.printStackTrace();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to create connection: {}", e.getMessage(), e);
        }

        LOGGER.info("=== Simple D-Bus Connection Test Complete ===");
    }
}
