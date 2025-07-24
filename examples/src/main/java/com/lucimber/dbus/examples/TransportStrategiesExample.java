/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example demonstrating different D-Bus connection types and configurations.
 *
 * <p>This example shows how to: - Connect to session and system buses - Configure connection
 * timeouts and options - Handle different connection scenarios - Use connection configuration
 * builders
 */
public class TransportStrategiesExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportStrategiesExample.class);

    public static void main(String[] args) throws Exception {
        System.out.println("🚀 D-Bus Connection Examples");
        System.out.println("============================");

        // Example 1: Standard bus connections
        demonstrateStandardConnections();

        // Example 2: Custom connection configuration
        demonstrateCustomConfiguration();

        System.out.println("🏁 Connection examples completed!");
    }

    /** Demonstrates standard D-Bus bus connections. */
    private static void demonstrateStandardConnections() throws Exception {
        System.out.println("\n📋 Example 1: Standard D-Bus Connections");
        System.out.println("========================================");

        // Session bus connection (most common for user applications)
        System.out.println("🔗 Connecting to session bus...");
        Connection sessionConnection = NettyConnection.newSessionBusConnection();

        try {
            sessionConnection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);
            System.out.println("✅ Session bus connected successfully!");
            System.out.println("   Connection state: " + sessionConnection.getState());
            System.out.println("   State: " + sessionConnection.getState());

            testBasicDbusCall(sessionConnection, "Session Bus");

        } catch (Exception e) {
            System.out.println("⚠️  Session bus connection failed: " + e.getMessage());
            LOGGER.debug("Session bus connection error", e);
        } finally {
            sessionConnection.close();
            System.out.println("🔌 Session bus connection closed");
        }

        // System bus connection (requires appropriate permissions)
        System.out.println("\n🔗 Connecting to system bus...");
        Connection systemConnection = NettyConnection.newSystemBusConnection();

        try {
            systemConnection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);
            System.out.println("✅ System bus connected successfully!");
            System.out.println("   Connection state: " + systemConnection.getState());
            System.out.println("   State: " + systemConnection.getState());

            testBasicDbusCall(systemConnection, "System Bus");

        } catch (Exception e) {
            System.out.println("⚠️  System bus connection failed: " + e.getMessage());
            System.out.println("   (This is normal if you don't have system bus permissions)");
            LOGGER.debug("System bus connection error", e);
        } finally {
            systemConnection.close();
            System.out.println("🔌 System bus connection closed");
        }
    }

    /** Demonstrates custom connection configuration options. */
    private static void demonstrateCustomConfiguration() throws Exception {
        System.out.println("\n📋 Example 2: Custom Connection Configuration");
        System.out.println("============================================");

        // Create a connection with custom timeout settings
        System.out.println("⚙️  Creating connection with custom configuration...");

        ConnectionConfig customConfig =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withMethodCallTimeout(Duration.ofSeconds(15))
                        .withReadTimeout(Duration.ofSeconds(30))
                        .withWriteTimeout(Duration.ofSeconds(5))
                        .withHealthCheckEnabled(true)
                        .withHealthCheckInterval(Duration.ofSeconds(60))
                        .withAutoReconnectEnabled(true)
                        .withMaxReconnectAttempts(5)
                        .withReconnectInitialDelay(Duration.ofSeconds(1))
                        .withReconnectMaxDelay(Duration.ofMinutes(2))
                        .build();

        System.out.println("📝 Custom configuration created:");
        System.out.println("   Connect timeout: " + customConfig.getConnectTimeout());
        System.out.println("   Method call timeout: " + customConfig.getMethodCallTimeout());
        System.out.println(
                "   Health checks: "
                        + (customConfig.isHealthCheckEnabled() ? "enabled" : "disabled"));
        System.out.println(
                "   Auto-reconnect: "
                        + (customConfig.isAutoReconnectEnabled() ? "enabled" : "disabled"));
        System.out.println("   Max reconnect attempts: " + customConfig.getMaxReconnectAttempts());

        // Note: NettyConnection.newSessionBusConnection() uses default config
        // Custom config would be used with NettyConnection.create(config) if that API existed
        Connection connection = NettyConnection.newSessionBusConnection();

        try {
            System.out.println("🔗 Connecting with session bus connection...");
            connection
                    .connect()
                    .toCompletableFuture()
                    .get(customConfig.getConnectTimeout().toMillis(), TimeUnit.MILLISECONDS);

            System.out.println("✅ Custom configured connection established!");

            testBasicDbusCall(connection, "Custom Configured Connection");

        } catch (Exception e) {
            System.out.println("⚠️  Custom connection failed: " + e.getMessage());
            LOGGER.debug("Custom connection error", e);
        } finally {
            connection.close();
            System.out.println("🔌 Custom connection closed");
        }

        // Demonstrate timeout configuration effects
        System.out.println("\n⏱️  Connection Timeout Configurations:");
        System.out.println(
                "   • Connect timeout: Controls how long to wait for initial connection");
        System.out.println("   • Method call timeout: How long to wait for D-Bus method responses");
        System.out.println("   • Read/Write timeouts: Network I/O operation timeouts");
        System.out.println("   • Health check interval: How often to verify connection health");
        System.out.println("   • Reconnect settings: Automatic reconnection behavior");
    }

    /** Helper method to test basic D-Bus functionality. */
    private static void testBasicDbusCall(Connection connection, String connectionType) {
        try {
            System.out.printf("📞 Testing %s with basic D-Bus call...%n", connectionType);

            OutboundMethodCall call =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("GetId"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> response =
                    connection.sendRequest(call).toCompletableFuture();
            InboundMessage reply = response.get(5, TimeUnit.SECONDS);

            System.out.printf(
                    "✅ %s call successful! Reply serial: %d%n",
                    connectionType, reply.getSerial().getDelegate());

        } catch (Exception e) {
            System.out.printf("⚠️  %s call failed: %s%n", connectionType, e.getMessage());
            LOGGER.debug(connectionType + " call error", e);
        }
    }
}
