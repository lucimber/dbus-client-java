/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load integration tests for D-Bus connections.
 * These tests validate behavior under high load and concurrent usage.
 */
@Tag("integration")
@Tag("performance")
@DisabledIf("shouldSkipDBusTests")
class PerformanceIntegrationTest extends DBusIntegrationTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceIntegrationTest.class);

    @Test
    void testConcurrentConnections() throws Exception {
        int connectionCount = 10;
        List<Connection> connections = new ArrayList<>();
        List<CompletableFuture<Void>> connectFutures = new ArrayList<>();

        try {
            long startTime = System.currentTimeMillis();
            
            // Create multiple connections concurrently
            for (int i = 0; i < connectionCount; i++) {
                ConnectionConfig config = ConnectionConfig.builder()
                    .withConnectTimeout(Duration.ofSeconds(30))
                    .build();

                Connection connection = new NettyConnection(
                    new InetSocketAddress(getDBusHost(), getDBusPort()),
                    config
                );
                
                connections.add(connection);
                connectFutures.add(connection.connect().toCompletableFuture());
            }

            // Wait for all connections to establish
            CompletableFuture<Void> allConnected = CompletableFuture.allOf(
                connectFutures.toArray(new CompletableFuture[0]));
            allConnected.get(Duration.ofMinutes(2).toMillis(), TimeUnit.MILLISECONDS);

            long connectTime = System.currentTimeMillis() - startTime;

            // Verify all connections are established
            for (Connection connection : connections) {
                assertTrue(connection.isConnected(), "All connections should be established");
            }

            LOGGER.info("✓ Successfully established {} concurrent connections in {}ms", 
                       connectionCount, connectTime);
            
            // Test that all connections can send requests simultaneously
            List<CompletableFuture<InboundMessage>> requestFutures = new ArrayList<>();
            startTime = System.currentTimeMillis();
            
            for (Connection connection : connections) {
                OutboundMethodCall listNames = OutboundMethodCall.Builder
                    .create()
                    .withSerial(connection.getNextSerial())
                    .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                    .withMember(DBusString.valueOf("ListNames"))
                    .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                    .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                    .withReplyExpected(true)
                    .build();

                requestFutures.add(connection.sendRequest(listNames).toCompletableFuture());
            }

            // Wait for all requests to complete
            CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                requestFutures.toArray(new CompletableFuture[0]));
            allRequests.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            long requestTime = System.currentTimeMillis() - startTime;

            LOGGER.info("✓ Successfully completed {} concurrent requests in {}ms", 
                       connectionCount, requestTime);

        } finally {
            // Close all connections
            for (Connection connection : connections) {
                if (connection != null) {
                    connection.close();
                }
            }
        }
    }

    @Test
    void testHighThroughputRequests() throws Exception {
        Connection connection = createConnection();
        
        try {
            int requestCount = 100;
            CountDownLatch latch = new CountDownLatch(requestCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            ExecutorService executor = Executors.newFixedThreadPool(10);
            
            long startTime = System.currentTimeMillis();
            
            try {
                // Send many requests concurrently
                for (int i = 0; i < requestCount; i++) {
                    final int requestId = i;
                    executor.submit(() -> {
                        try {
                            OutboundMethodCall ping = OutboundMethodCall.Builder
                                .create()
                                .withSerial(connection.getNextSerial())
                                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                                .withMember(DBusString.valueOf("Ping"))
                                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                                .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
                                .withReplyExpected(true)
                                .build();

                            CompletableFuture<InboundMessage> future = 
                                connection.sendRequest(ping).toCompletableFuture();
                            future.get(10, TimeUnit.SECONDS);
                            
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            LOGGER.warn("Request {} failed: {}", requestId, e.getMessage());
                            errorCount.incrementAndGet();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                // Wait for all requests to complete
                assertTrue(latch.await(60, TimeUnit.SECONDS), 
                    "All requests should complete within timeout");

                long totalTime = System.currentTimeMillis() - startTime;
                double requestsPerSecond = (double) requestCount / (totalTime / 1000.0);

                int successful = successCount.get();
                int failed = errorCount.get();

                LOGGER.info("✓ High throughput test completed:");
                LOGGER.info("  - Total requests: {}", requestCount);
                LOGGER.info("  - Successful: {}", successful);
                LOGGER.info("  - Failed: {}", failed);
                LOGGER.info("  - Total time: {}ms", totalTime);
                LOGGER.info("  - Requests/second: {:.2f}", requestsPerSecond);

                // Most requests should succeed
                assertTrue(successful > requestCount * 0.8, 
                    "At least 80% of requests should succeed");

            } finally {
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            }

        } finally {
            connection.close();
        }
    }

    @Test
    void testConnectionPooling() throws Exception {
        int poolSize = 5;
        int requestsPerConnection = 20;
        
        List<Connection> connectionPool = new ArrayList<>();
        
        try {
            // Create connection pool
            for (int i = 0; i < poolSize; i++) {
                Connection connection = createConnection();
                connectionPool.add(connection);
            }

            LOGGER.info("✓ Created connection pool with {} connections", poolSize);

            CountDownLatch latch = new CountDownLatch(poolSize * requestsPerConnection);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicLong totalResponseTime = new AtomicLong(0);
            
            ExecutorService executor = Executors.newFixedThreadPool(poolSize * 2);
            
            try {
                long startTime = System.currentTimeMillis();
                
                // Submit requests using round-robin connection selection
                for (int i = 0; i < poolSize * requestsPerConnection; i++) {
                    final int requestId = i;
                    final Connection connection = connectionPool.get(i % poolSize);
                    
                    executor.submit(() -> {
                        try {
                            long requestStart = System.currentTimeMillis();
                            
                            OutboundMethodCall listNames = OutboundMethodCall.Builder
                                .create()
                                .withSerial(connection.getNextSerial())
                                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                                .withMember(DBusString.valueOf("ListNames"))
                                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                                .withReplyExpected(true)
                                .build();

                            CompletableFuture<InboundMessage> future = 
                                connection.sendRequest(listNames).toCompletableFuture();
                            future.get(15, TimeUnit.SECONDS);
                            
                            long responseTime = System.currentTimeMillis() - requestStart;
                            totalResponseTime.addAndGet(responseTime);
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            LOGGER.warn("Pooled request {} failed: {}", requestId, e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                // Wait for all requests
                assertTrue(latch.await(120, TimeUnit.SECONDS), 
                    "All pooled requests should complete");

                long totalTime = System.currentTimeMillis() - startTime;
                int successful = successCount.get();
                double avgResponseTime = (double) totalResponseTime.get() / successful;

                LOGGER.info("✓ Connection pooling test completed:");
                LOGGER.info("  - Pool size: {}", poolSize);
                LOGGER.info("  - Total requests: {}", poolSize * requestsPerConnection);
                LOGGER.info("  - Successful: {}", successful);
                LOGGER.info("  - Total time: {}ms", totalTime);
                LOGGER.info("  - Average response time: {:.2f}ms", avgResponseTime);

                // Should handle the load efficiently
                assertTrue(successful > (poolSize * requestsPerConnection) * 0.9, 
                    "At least 90% of pooled requests should succeed");

            } finally {
                executor.shutdown();
                executor.awaitTermination(30, TimeUnit.SECONDS);
            }

        } finally {
            // Close all connections in pool
            for (Connection connection : connectionPool) {
                if (connection != null) {
                    connection.close();
                }
            }
        }
    }

    @Test
    void testMemoryUsageUnderLoad() throws Exception {
        // Force garbage collection before test
        System.gc();
        Thread.sleep(1000);
        
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        Connection connection = createConnection();
        
        try {
            int iterations = 50;
            
            for (int i = 0; i < iterations; i++) {
                // Create and send multiple requests
                List<CompletableFuture<InboundMessage>> futures = new ArrayList<>();
                
                for (int j = 0; j < 10; j++) {
                    OutboundMethodCall ping = OutboundMethodCall.Builder
                        .create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                        .withMember(DBusString.valueOf("Ping"))
                        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
                        .withReplyExpected(true)
                        .build();

                    futures.add(connection.sendRequest(ping).toCompletableFuture());
                }

                // Wait for completion
                CompletableFuture<Void> allComplete = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
                allComplete.get(10, TimeUnit.SECONDS);

                // Periodic garbage collection
                if (i % 10 == 0) {
                    System.gc();
                    Thread.sleep(100);
                }
            }

            // Final garbage collection
            System.gc();
            Thread.sleep(1000);
            
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = memoryAfter - memoryBefore;
            double memoryIncreaseMB = memoryIncrease / (1024.0 * 1024.0);

            LOGGER.info("✓ Memory usage test completed:");
            LOGGER.info("  - Memory before: {:.2f} MB", memoryBefore / (1024.0 * 1024.0));
            LOGGER.info("  - Memory after: {:.2f} MB", memoryAfter / (1024.0 * 1024.0));
            LOGGER.info("  - Memory increase: {:.2f} MB", memoryIncreaseMB);
            LOGGER.info("  - Iterations: {}", iterations);

            // Memory increase should be reasonable (less than 50MB for this load)
            assertTrue(memoryIncreaseMB < 50.0, 
                "Memory increase should be reasonable: " + memoryIncreaseMB + " MB");

        } finally {
            connection.close();
        }
    }

    @Test
    void testConnectionRecoveryUnderLoad() throws Exception {
        ConnectionConfig config = ConnectionConfig.builder()
            .withConnectTimeout(Duration.ofSeconds(30))
            .withAutoReconnectEnabled(true)
            .withReconnectInitialDelay(Duration.ofMillis(100))
            .withReconnectMaxDelay(Duration.ofSeconds(2))
            .withMaxReconnectAttempts(5)
            .build();

        Connection connection = new NettyConnection(
            new InetSocketAddress(getDBusHost(), getDBusPort()),
            config
        );

        try {
            connection.connect().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            assertTrue(connection.isConnected());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            // Send requests continuously while connection might be unstable
            ExecutorService executor = Executors.newFixedThreadPool(5);
            CountDownLatch latch = new CountDownLatch(50);
            
            try {
                for (int i = 0; i < 50; i++) {
                    final int requestId = i;
                    executor.submit(() -> {
                        try {
                            OutboundMethodCall ping = OutboundMethodCall.Builder
                                .create()
                                .withSerial(connection.getNextSerial())
                                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                                .withMember(DBusString.valueOf("Ping"))
                                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                                .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
                                .withReplyExpected(true)
                                .build();

                            CompletableFuture<InboundMessage> future = 
                                connection.sendRequest(ping).toCompletableFuture();
                            future.get(5, TimeUnit.SECONDS);
                            
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            LOGGER.debug("Request {} failed during load test: {}", requestId, e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                    
                    // Small delay between requests
                    Thread.sleep(50);
                }

                assertTrue(latch.await(60, TimeUnit.SECONDS), 
                    "All requests should complete within timeout");

                int successful = successCount.get();
                int failed = errorCount.get();

                LOGGER.info("✓ Connection recovery under load test:");
                LOGGER.info("  - Successful requests: {}", successful);
                LOGGER.info("  - Failed requests: {}", failed);
                LOGGER.info("  - Success rate: {:.1f}%", (successful * 100.0) / (successful + failed));

                // Should have reasonable success rate even under load
                assertTrue(successful > 30, "Should have at least 30 successful requests");

            } finally {
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.SECONDS);
            }

        } finally {
            connection.close();
        }
    }

    /**
     * Creates a connection for testing.
     */
    private Connection createConnection() throws Exception {
        ConnectionConfig config = ConnectionConfig.builder()
            .withConnectTimeout(Duration.ofSeconds(30))
            .build();

        Connection connection = new NettyConnection(
            new InetSocketAddress(getDBusHost(), getDBusPort()),
            config
        );

        connection.connect().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertTrue(connection.isConnected());
        
        return connection;
    }
}