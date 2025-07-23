/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConnectionConfigTest {

    @Test
    void testDefaultConfig() {
        ConnectionConfig config = ConnectionConfig.defaultConfig();

        assertEquals(Duration.ofSeconds(30), config.getMethodCallTimeout());
        assertEquals(Duration.ofSeconds(10), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(60), config.getReadTimeout());
        assertEquals(Duration.ofSeconds(10), config.getWriteTimeout());
    }

    @Test
    void testBuilderWithCustomTimeouts() {
        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withMethodCallTimeout(Duration.ofSeconds(15))
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withReadTimeout(Duration.ofSeconds(30))
                        .withWriteTimeout(Duration.ofSeconds(8))
                        .build();

        assertEquals(Duration.ofSeconds(15), config.getMethodCallTimeout());
        assertEquals(Duration.ofSeconds(5), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(30), config.getReadTimeout());
        assertEquals(Duration.ofSeconds(8), config.getWriteTimeout());
    }

    @Test
    void testTimeoutInMilliseconds() {
        ConnectionConfig config =
                ConnectionConfig.builder().withMethodCallTimeout(Duration.ofMillis(500)).build();

        assertEquals(500, config.getMethodCallTimeoutMs());
    }

    @Test
    void testBuilderValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withMethodCallTimeout(Duration.ZERO));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withMethodCallTimeout(Duration.ofSeconds(-1)));

        assertThrows(
                NullPointerException.class,
                () -> ConnectionConfig.builder().withMethodCallTimeout(null));
    }

    @Test
    void testBuilderPartialConfiguration() {
        ConnectionConfig config =
                ConnectionConfig.builder().withMethodCallTimeout(Duration.ofSeconds(20)).build();

        assertEquals(Duration.ofSeconds(20), config.getMethodCallTimeout());
        // Other values should be defaults
        assertEquals(Duration.ofSeconds(10), config.getConnectTimeout());
        assertEquals(Duration.ofSeconds(60), config.getReadTimeout());
        assertEquals(Duration.ofSeconds(10), config.getWriteTimeout());
    }

    @Test
    void testEqualsAndHashCode() {
        ConnectionConfig config1 =
                ConnectionConfig.builder()
                        .withMethodCallTimeout(Duration.ofSeconds(15))
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .build();

        ConnectionConfig config2 =
                ConnectionConfig.builder()
                        .withMethodCallTimeout(Duration.ofSeconds(15))
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testToString() {
        ConnectionConfig config = ConnectionConfig.defaultConfig();
        String str = config.toString();

        assertTrue(str.contains("ConnectionConfig"));
        assertTrue(str.contains("methodCallTimeout"));
        assertTrue(str.contains("connectTimeout"));
        assertTrue(str.contains("healthCheckEnabled"));
        assertTrue(str.contains("autoReconnectEnabled"));
    }

    @Test
    void testHealthCheckConfiguration() {
        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withHealthCheckEnabled(true)
                        .withHealthCheckInterval(Duration.ofSeconds(20))
                        .withHealthCheckTimeout(Duration.ofSeconds(3))
                        .build();

        assertTrue(config.isHealthCheckEnabled());
        assertEquals(Duration.ofSeconds(20), config.getHealthCheckInterval());
        assertEquals(Duration.ofSeconds(3), config.getHealthCheckTimeout());
    }

    @Test
    void testAutoReconnectConfiguration() {
        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withAutoReconnectEnabled(true)
                        .withReconnectInitialDelay(Duration.ofSeconds(2))
                        .withReconnectMaxDelay(Duration.ofMinutes(10))
                        .withReconnectBackoffMultiplier(1.5)
                        .withMaxReconnectAttempts(5)
                        .build();

        assertTrue(config.isAutoReconnectEnabled());
        assertEquals(Duration.ofSeconds(2), config.getReconnectInitialDelay());
        assertEquals(Duration.ofMinutes(10), config.getReconnectMaxDelay());
        assertEquals(1.5, config.getReconnectBackoffMultiplier());
        assertEquals(5, config.getMaxReconnectAttempts());
    }

    @Test
    void testDefaultHealthAndReconnectSettings() {
        ConnectionConfig config = ConnectionConfig.defaultConfig();

        // Health check defaults
        assertTrue(config.isHealthCheckEnabled());
        assertEquals(Duration.ofSeconds(30), config.getHealthCheckInterval());
        assertEquals(Duration.ofSeconds(5), config.getHealthCheckTimeout());

        // Auto-reconnect defaults
        assertTrue(config.isAutoReconnectEnabled());
        assertEquals(Duration.ofSeconds(1), config.getReconnectInitialDelay());
        assertEquals(Duration.ofMinutes(5), config.getReconnectMaxDelay());
        assertEquals(2.0, config.getReconnectBackoffMultiplier());
        assertEquals(10, config.getMaxReconnectAttempts());
    }

    @Test
    void testHealthCheckValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withHealthCheckInterval(Duration.ZERO));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withHealthCheckTimeout(Duration.ofSeconds(-1)));

        assertThrows(
                NullPointerException.class,
                () -> ConnectionConfig.builder().withHealthCheckInterval(null));
    }

    @Test
    void testReconnectValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withReconnectInitialDelay(Duration.ZERO));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withReconnectMaxDelay(Duration.ofSeconds(-1)));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withReconnectBackoffMultiplier(0.5));

        assertThrows(
                IllegalArgumentException.class,
                () -> ConnectionConfig.builder().withMaxReconnectAttempts(-1));
    }
}
