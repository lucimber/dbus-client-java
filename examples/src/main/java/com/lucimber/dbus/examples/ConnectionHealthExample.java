/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionEvent;
import com.lucimber.dbus.connection.ConnectionEventListener;
import com.lucimber.dbus.connection.ConnectionState;
import com.lucimber.dbus.netty.NettyConnection;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example demonstrating connection health monitoring and event handling.
 *
 * <p>This example shows how to: - Monitor connection state changes - Handle connection health
 * events - Configure connection timeouts and retry logic - Implement custom event listeners
 */
public class ConnectionHealthExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHealthExample.class);

    public static void main(String[] args) throws Exception {
        System.out.println("🔍 D-Bus Connection Health Monitoring Example");
        System.out.println("===============================================");

        // Demonstrate connection configuration options
        System.out.println("⚙️  Creating custom connection configuration...");
        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(10))
                        .withHealthCheckInterval(Duration.ofSeconds(30))
                        .withMaxReconnectAttempts(3)
                        .withAutoReconnectEnabled(true)
                        .withHealthCheckEnabled(true)
                        .build();

        System.out.println("📝 Configuration details:");
        System.out.println("   Connect timeout: " + config.getConnectTimeout());
        System.out.println("   Health check interval: " + config.getHealthCheckInterval());
        System.out.println("   Max reconnect attempts: " + config.getMaxReconnectAttempts());
        System.out.println("   Auto-reconnect enabled: " + config.isAutoReconnectEnabled());
        System.out.println("   Health checks enabled: " + config.isHealthCheckEnabled());

        // Create connection (configuration is shown for demonstration)
        // Note: NettyConnection.newSystemBusConnection() uses its own internal configuration
        Connection connection = NettyConnection.newSystemBusConnection();

        // Add comprehensive event listener
        ConnectionHealthListener healthListener = new ConnectionHealthListener();
        connection.addConnectionEventListener(healthListener);

        try {
            // Demonstrate connection lifecycle
            System.out.println("\n📡 Connecting to system D-Bus...");
            connection.connect().toCompletableFuture().get(15, TimeUnit.SECONDS);

            System.out.println("✅ Connected successfully!");
            System.out.println("🔧 Connection state: " + connection.getState());
            System.out.println("📊 Current state: " + connection.getState());

            // Wait for health events
            System.out.println("\n⏳ Monitoring connection health for 60 seconds...");
            System.out.println("   (Try disconnecting D-Bus daemon to see reconnection logic)");

            Thread.sleep(60000);

            // Demonstrate graceful shutdown
            System.out.println("\n🔚 Initiating graceful shutdown...");
            connection.close();

            // Wait for shutdown events
            healthListener.awaitShutdown(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.error("Connection health example failed", e);
            System.err.println("❌ Example failed: " + e.getMessage());
        } finally {
            if (connection.getState() != ConnectionState.DISCONNECTED) {
                try {
                    connection.close();
                } catch (Exception e) {
                    LOGGER.warn("Error closing connection", e);
                }
            }
        }

        System.out.println("🏁 Connection health monitoring example completed");
    }

    /** Custom connection event listener that demonstrates comprehensive event handling. */
    private static class ConnectionHealthListener implements ConnectionEventListener {

        private final CountDownLatch shutdownLatch = new CountDownLatch(1);

        @Override
        public void onConnectionEvent(Connection connection, ConnectionEvent event) {
            switch (event.getType()) {
                case STATE_CHANGED:
                    handleStateChange(event);
                    break;
                case HEALTH_CHECK_FAILURE:
                    handleHealthCheckFailed(event);
                    break;
                case RECONNECTION_ATTEMPT:
                    handleReconnectionStarted(event);
                    break;
                case RECONNECTION_SUCCESS:
                    handleReconnectionSucceeded(event);
                    break;
                case RECONNECTION_FAILURE:
                    handleReconnectionFailed(event);
                    break;
                default:
                    LOGGER.debug("Received event: {}", event.getType());
            }
        }

        private void handleStateChange(ConnectionEvent event) {
            ConnectionState oldState = event.getOldState().orElse(null);
            ConnectionState newState = event.getNewState().orElse(null);

            System.out.printf("🔄 State change: %s → %s%n", oldState, newState);

            switch (newState) {
                case CONNECTING:
                    System.out.println("   ⏳ Establishing connection...");
                    break;
                case AUTHENTICATING:
                    System.out.println("   🔒 Authenticating...");
                    break;
                case CONNECTED:
                    System.out.println("   ✅ Connection established successfully");
                    break;
                case UNHEALTHY:
                    System.out.println("   ⚠️ Connection unhealthy");
                    break;
                case RECONNECTING:
                    System.out.println("   🔄 Attempting to reconnect...");
                    break;
                case FAILED:
                    System.out.println("   ❌ Connection failed permanently");
                    break;
                case DISCONNECTED:
                    System.out.println("   🔌 Connection closed");
                    shutdownLatch.countDown();
                    break;
                default:
                    break;
            }
        }

        private void handleHealthCheckFailed(ConnectionEvent event) {
            System.out.println("⚠️  Health check failed");
            // Note: Failure information not available in current ConnectionEvent API
            System.out.println("   Health check monitoring active");
        }

        private void handleReconnectionStarted(ConnectionEvent event) {
            System.out.println("🔄 Reconnection started");
            // Note: Attempt number not available in current ConnectionEvent API
            System.out.println("   Reconnection in progress...");
        }

        private void handleReconnectionSucceeded(ConnectionEvent event) {
            System.out.println("✅ Reconnection succeeded");
            // Note: Attempt number not available in current ConnectionEvent API
            System.out.println("   Reconnection successful");
        }

        private void handleReconnectionFailed(ConnectionEvent event) {
            System.out.println("❌ Reconnection failed");
            // Note: Attempt number and failure info not available in current ConnectionEvent API
            System.out.println("   Reconnection attempts exhausted");
        }

        public void awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
            shutdownLatch.await(timeout, unit);
        }
    }
}
