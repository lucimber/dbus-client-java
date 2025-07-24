/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.chaos;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionEventListener;
import com.lucimber.dbus.connection.ConnectionEventType;
import com.lucimber.dbus.connection.ConnectionState;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chaos engineering tests to verify D-Bus client resilience under adverse conditions. These tests
 * simulate various failure scenarios to ensure robust behavior.
 */
@Tag("chaos")
@DisabledIf("shouldSkipChaosTests")
public class DBusChaosTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBusChaosTest.class);

    private static final Duration CHAOS_TIMEOUT = Duration.ofMinutes(2);

    @Test
    void testResilenceToNetworkPartition() throws Exception {
        LOGGER.info("Testing resilience to network partition");

        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withAutoReconnectEnabled(true)
                        .withReconnectInitialDelay(Duration.ofMillis(100))
                        .withReconnectMaxDelay(Duration.ofSeconds(2))
                        .withMaxReconnectAttempts(5)
                        .withHealthCheckEnabled(true)
                        .withHealthCheckInterval(Duration.ofSeconds(1))
                        .build();

        Connection connection =
                new NettyConnection(new InetSocketAddress("localhost", 12345), config);

        AtomicInteger reconnectAttempts = new AtomicInteger(0);
        AtomicReference<ConnectionState> lastState = new AtomicReference<>();
        CountDownLatch reconnectLatch = new CountDownLatch(1);

        ConnectionEventListener listener =
                (conn, event) -> {
                    lastState.set(conn.getState());
                    if (event.getType() == ConnectionEventType.RECONNECTION_ATTEMPT) {
                        reconnectAttempts.incrementAndGet();
                        reconnectLatch.countDown();
                    }
                };

        try {
            connection.addConnectionEventListener(listener);

            // Initial connection
            connection.connect().toCompletableFuture().get(30, TimeUnit.SECONDS);
            assertTrue(connection.isConnected());

            // Simulate network partition by stopping Docker container
            simulateNetworkPartition();

            // Wait for reconnection attempts
            assertTrue(reconnectLatch.await(30, TimeUnit.SECONDS), "Should attempt reconnection");
            assertTrue(reconnectAttempts.get() > 0, "Should have attempted reconnection");

            LOGGER.info(
                    "Network partition test completed - Reconnect attempts: {}",
                    reconnectAttempts.get());

        } finally {
            connection.close();
        }
    }

    @Test
    void testRapidConnectionCycling() throws Exception {
        LOGGER.info("Testing rapid connection cycling");

        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(2))
                        .withAutoReconnectEnabled(false)
                        .build();

        int cycleCount = 20;
        AtomicInteger successfulConnections = new AtomicInteger(0);
        AtomicInteger failedConnections = new AtomicInteger(0);

        for (int i = 0; i < cycleCount; i++) {
            Connection connection =
                    new NettyConnection(new InetSocketAddress("localhost", 12345), config);

            try {
                CompletableFuture<Void> connectFuture = connection.connect().toCompletableFuture();
                connectFuture.get(5, TimeUnit.SECONDS);

                if (connection.isConnected()) {
                    successfulConnections.incrementAndGet();

                    // Perform a quick operation
                    OutboundMethodCall methodCall = createTestMethodCall();
                    connection
                            .sendRequest(methodCall)
                            .toCompletableFuture()
                            .get(2, TimeUnit.SECONDS);
                }

            } catch (Exception e) {
                failedConnections.incrementAndGet();
                LOGGER.debug("Connection cycle {} failed: {}", i, e.getMessage());
            } finally {
                connection.close();
            }

            // Small delay between cycles
            Thread.sleep(50);
        }

        double successRate = (double) successfulConnections.get() / cycleCount;

        LOGGER.info("Rapid cycling test completed:");
        LOGGER.info("  Cycles: {}", cycleCount);
        LOGGER.info("  Successful: {}", successfulConnections.get());
        LOGGER.info("  Failed: {}", failedConnections.get());
        LOGGER.info("  Success rate: {:.2f}%", successRate * 100);

        // Should have reasonable success rate even under stress
        assertTrue(successRate >= 0.7, "Success rate should be at least 70% under rapid cycling");
    }

    @Test
    void testConcurrentStressWithRandomFailures() throws Exception {
        LOGGER.info("Testing concurrent stress with random failures");

        int threadCount = 10;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);

        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        try {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;

                executor.submit(
                        () -> {
                            try {
                                Connection connection =
                                        new NettyConnection(
                                                new InetSocketAddress("localhost", 12345),
                                                ConnectionConfig.defaultConfig());

                                try {
                                    connection
                                            .connect()
                                            .toCompletableFuture()
                                            .get(10, TimeUnit.SECONDS);

                                    for (int op = 0; op < operationsPerThread; op++) {
                                        totalOperations.incrementAndGet();

                                        try {
                                            // Random delay to simulate varying load
                                            Thread.sleep((long) (Math.random() * 10));

                                            OutboundMethodCall methodCall = createTestMethodCall();
                                            InboundMessage response =
                                                    connection
                                                            .sendRequest(methodCall)
                                                            .toCompletableFuture()
                                                            .get(5, TimeUnit.SECONDS);

                                            assertNotNull(response);
                                            successfulOperations.incrementAndGet();

                                        } catch (Exception e) {
                                            failedOperations.incrementAndGet();
                                            LOGGER.debug(
                                                    "Operation failed in thread {}: {}",
                                                    threadId,
                                                    e.getMessage());
                                        }
                                    }

                                } finally {
                                    connection.close();
                                }

                            } catch (Exception e) {
                                LOGGER.error("Thread {} failed: {}", threadId, e.getMessage());
                            } finally {
                                completionLatch.countDown();
                            }
                        });
            }

            assertTrue(completionLatch.await(CHAOS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

            double successRate = (double) successfulOperations.get() / totalOperations.get();

            LOGGER.info("Concurrent stress test completed:");
            LOGGER.info("  Threads: {}", threadCount);
            LOGGER.info("  Total operations: {}", totalOperations.get());
            LOGGER.info("  Successful: {}", successfulOperations.get());
            LOGGER.info("  Failed: {}", failedOperations.get());
            LOGGER.info("  Success rate: {:.2f}%", successRate * 100);

            // Should maintain reasonable success rate under concurrent stress
            assertTrue(
                    successRate >= 0.8,
                    "Success rate should be at least 80% under concurrent stress");

        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void testResourceExhaustionResilience() throws Exception {
        LOGGER.info("Testing resource exhaustion resilience");

        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(1))
                        .withMethodCallTimeout(Duration.ofSeconds(2))
                        .build();

        // Create many connections to exhaust resources
        int connectionCount = 100;
        Connection[] connections = new Connection[connectionCount];
        AtomicInteger successfulConnections = new AtomicInteger(0);
        AtomicInteger resourceErrors = new AtomicInteger(0);

        try {
            for (int i = 0; i < connectionCount; i++) {
                try {
                    connections[i] =
                            new NettyConnection(new InetSocketAddress("localhost", 12345), config);

                    CompletableFuture<Void> connectFuture =
                            connections[i].connect().toCompletableFuture();
                    connectFuture.get(3, TimeUnit.SECONDS);

                    if (connections[i].isConnected()) {
                        successfulConnections.incrementAndGet();
                    }

                } catch (Exception e) {
                    resourceErrors.incrementAndGet();
                    LOGGER.debug(
                            "Resource exhaustion error at connection {}: {}", i, e.getMessage());
                    // Expected under resource exhaustion
                }
            }

            LOGGER.info("Resource exhaustion test completed:");
            LOGGER.info("  Attempted connections: {}", connectionCount);
            LOGGER.info("  Successful: {}", successfulConnections.get());
            LOGGER.info("  Resource errors: {}", resourceErrors.get());

            // Should be able to create at least some connections
            assertTrue(successfulConnections.get() > 0, "Should create at least some connections");

            // Should handle resource exhaustion gracefully (not crash)
            assertTrue(resourceErrors.get() > 0, "Should encounter resource limits");

        } finally {
            // Clean up connections
            for (Connection connection : connections) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }
            }
        }
    }

    @Test
    void testSlowConnectionResponse() throws Exception {
        LOGGER.info("Testing slow connection response handling");

        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(2))
                        .withMethodCallTimeout(Duration.ofSeconds(1))
                        .withHealthCheckEnabled(true)
                        .withHealthCheckTimeout(Duration.ofMillis(500))
                        .build();

        Connection connection =
                new NettyConnection(new InetSocketAddress("localhost", 12345), config);

        AtomicBoolean timeoutOccurred = new AtomicBoolean(false);

        try {
            connection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);
            assertTrue(connection.isConnected());

            // Perform operations with tight timeouts
            int operationCount = 10;
            int timeouts = 0;

            for (int i = 0; i < operationCount; i++) {
                try {
                    OutboundMethodCall methodCall = createTestMethodCall();
                    CompletableFuture<InboundMessage> future =
                            connection.sendRequest(methodCall).toCompletableFuture();

                    // Use very short timeout to potentially trigger timeout handling
                    future.get(100, TimeUnit.MILLISECONDS);

                } catch (Exception e) {
                    timeouts++;
                    timeoutOccurred.set(true);
                    LOGGER.debug("Timeout occurred (expected): {}", e.getMessage());
                }

                Thread.sleep(100);
            }

            LOGGER.info("Slow response test completed:");
            LOGGER.info("  Operations: {}", operationCount);
            LOGGER.info("  Timeouts: {}", timeouts);
            LOGGER.info("  Connection still active: {}", connection.isConnected());

            // Connection should remain stable despite timeouts
            assertTrue(
                    connection.isConnected(), "Connection should remain active despite timeouts");

        } finally {
            connection.close();
        }
    }

    private OutboundMethodCall createTestMethodCall() {
        return OutboundMethodCall.Builder.create()
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("GetId"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withReplyExpected(true)
                .build();
    }

    private void simulateNetworkPartition() {
        try {
            // Simulate network issues by pausing the Docker container
            Process pauseProcess =
                    new ProcessBuilder("docker", "pause", "dbus-test-container").start();
            pauseProcess.waitFor(5, TimeUnit.SECONDS);

            // Wait a moment
            Thread.sleep(2000);

            // Resume the container
            Process resumeProcess =
                    new ProcessBuilder("docker", "unpause", "dbus-test-container").start();
            resumeProcess.waitFor(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            LOGGER.warn("Could not simulate network partition: {}", e.getMessage());
        }
    }

    /** Checks if chaos tests should be skipped. */
    static boolean shouldSkipChaosTests() {
        // Skip chaos tests if explicitly disabled or in CI without proper setup
        return "true".equals(System.getProperty("skip.chaos.tests"))
                || "true".equals(System.getenv("SKIP_CHAOS_TESTS"));
    }
}
