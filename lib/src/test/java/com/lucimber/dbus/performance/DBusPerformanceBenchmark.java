/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.performance;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarks for D-Bus client operations.
 * These tests measure throughput, latency, and resource usage.
 */
@Tag("performance")
@DisabledIf("shouldSkipPerformanceTests")
public class DBusPerformanceBenchmark {

  private static final Logger LOGGER = LoggerFactory.getLogger(DBusPerformanceBenchmark.class);
  
  private static final int WARMUP_ITERATIONS = 100;
  private static final int BENCHMARK_ITERATIONS = 1000;
  private static final Duration BENCHMARK_TIMEOUT = Duration.ofMinutes(5);
  
  private static Connection connection;
  private static ExecutorService executor;

  // Static initializer to check service availability early
  static {
    // This will be called when the class is loaded, before @DisabledIf is evaluated
  }

  @BeforeAll
  static void setUp() throws Exception {
    // Set up connection for benchmarks
    ConnectionConfig config = ConnectionConfig.builder()
        .withHealthCheckEnabled(false)  // Disable for cleaner benchmarks
        .withAutoReconnectEnabled(false)
        .build();

    connection = new NettyConnection(
        new InetSocketAddress("localhost", 12345),
        config
    );

    connection.connect().toCompletableFuture().get(30, TimeUnit.SECONDS);
    assertTrue(connection.isConnected());

    executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    LOGGER.info("Performance benchmark setup completed");
  }

  @AfterAll
  static void tearDown() {
    if (connection != null) {
      try {
        connection.close();
      } catch (Exception e) {
        // Ignore cleanup errors
      }
    }
    if (executor != null) {
      executor.shutdown();
    }
  }

  @Test
  void benchmarkMethodCallLatency() throws Exception {
    LOGGER.info("Starting method call latency benchmark");

    // Warmup
    performMethodCalls(WARMUP_ITERATIONS, false);

    // Actual benchmark
    BenchmarkResult result = performMethodCalls(BENCHMARK_ITERATIONS, true);

    LOGGER.info("Method Call Latency Benchmark Results:");
    LOGGER.info("  Total calls: {}", result.totalOperations);
    LOGGER.info("  Average latency: {:.2f} ms", result.averageLatencyMs);
    LOGGER.info("  Min latency: {:.2f} ms", result.minLatencyMs);
    LOGGER.info("  Max latency: {:.2f} ms", result.maxLatencyMs);
    LOGGER.info("  95th percentile: {:.2f} ms", result.p95LatencyMs);
    LOGGER.info("  Throughput: {:.2f} ops/sec", result.throughputOpsPerSec);

    // Basic assertions
    assertTrue(result.averageLatencyMs > 0);
    assertTrue(result.throughputOpsPerSec > 0);
    assertTrue(result.successRate >= 0.95, "Success rate should be at least 95%");
  }

  @Test
  void benchmarkConcurrentConnections() throws Exception {
    LOGGER.info("Starting concurrent connections benchmark");

    int connectionCount = 10;
    List<Connection> connections = new ArrayList<>();
    List<CompletableFuture<Void>> connectFutures = new ArrayList<>();

    long startTime = System.nanoTime();

    try {
      // Create connections concurrently
      for (int i = 0; i < connectionCount; i++) {
        Connection conn = new NettyConnection(
            new InetSocketAddress("localhost", 12345),
            ConnectionConfig.defaultConfig()
        );
        connections.add(conn);
        connectFutures.add(conn.connect().toCompletableFuture());
      }

      // Wait for all to connect
      CompletableFuture.allOf(connectFutures.toArray(new CompletableFuture[0]))
          .get(BENCHMARK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      long endTime = System.nanoTime();
      double connectionTimeMs = (endTime - startTime) / 1_000_000.0;

      LOGGER.info("Concurrent Connections Benchmark Results:");
      LOGGER.info("  Connections: {}", connectionCount);
      LOGGER.info("  Total time: {:.2f} ms", connectionTimeMs);
      LOGGER.info("  Average per connection: {:.2f} ms", connectionTimeMs / connectionCount);

      // Verify all connections are working
      for (Connection conn : connections) {
        assertTrue(conn.isConnected());
      }

    } finally {
      connections.forEach(conn -> {
        try {
          conn.close();
        } catch (Exception e) {
          // Ignore cleanup errors
        }
      });
    }
  }

  @Test
  void benchmarkHighThroughputMessages() throws Exception {
    LOGGER.info("Starting high throughput messages benchmark");

    int messageCount = 5000;
    int concurrencyLevel = 10;
    AtomicInteger completedMessages = new AtomicInteger(0);
    AtomicInteger failedMessages = new AtomicInteger(0);
    AtomicLong totalLatency = new AtomicLong(0);

    List<CompletableFuture<Void>> futures = new ArrayList<>();
    long startTime = System.nanoTime();

    // Send messages concurrently
    for (int i = 0; i < messageCount; i++) {
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          long messageStart = System.nanoTime();
          
          OutboundMethodCall methodCall = createTestMethodCall();
          InboundMessage response = connection.sendRequest(methodCall)
              .toCompletableFuture()
              .get(5, TimeUnit.SECONDS);
          
          assertNotNull(response);
          
          long messageEnd = System.nanoTime();
          totalLatency.addAndGet(messageEnd - messageStart);
          completedMessages.incrementAndGet();
          
        } catch (Exception e) {
          failedMessages.incrementAndGet();
          LOGGER.debug("Message failed: {}", e.getMessage());
        }
      }, executor);
      
      futures.add(future);
    }

    // Wait for all messages to complete
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .get(BENCHMARK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

    long endTime = System.nanoTime();
    double totalTimeMs = (endTime - startTime) / 1_000_000.0;
    double averageLatencyMs = (totalLatency.get() / 1_000_000.0) / completedMessages.get();
    double throughput = (completedMessages.get() * 1000.0) / totalTimeMs;
    double successRate = (double) completedMessages.get() / messageCount;

    LOGGER.info("High Throughput Messages Benchmark Results:");
    LOGGER.info("  Total messages: {}", messageCount);
    LOGGER.info("  Completed: {}", completedMessages.get());
    LOGGER.info("  Failed: {}", failedMessages.get());
    LOGGER.info("  Success rate: {:.2f}%", successRate * 100);
    LOGGER.info("  Total time: {:.2f} ms", totalTimeMs);
    LOGGER.info("  Average latency: {:.2f} ms", averageLatencyMs);
    LOGGER.info("  Throughput: {:.2f} messages/sec", throughput);

    assertTrue(successRate >= 0.90, "Success rate should be at least 90%");
    assertTrue(throughput > 100, "Throughput should be at least 100 messages/sec");
  }

  @Test
  @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
  void benchmarkMemoryUsage() throws Exception {
    LOGGER.info("Starting memory usage benchmark");

    Runtime runtime = Runtime.getRuntime();
    
    // Force garbage collection and measure baseline
    // Note: Explicit GC is necessary for reliable memory benchmarking
    System.gc();
    Thread.sleep(1000);
    long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

    // Perform operations and measure memory
    performMethodCalls(1000, false);
    
    // Note: Explicit GC is necessary for reliable memory benchmarking
    System.gc();
    Thread.sleep(1000);
    long afterOperationsMemory = runtime.totalMemory() - runtime.freeMemory();

    long memoryIncrease = afterOperationsMemory - baselineMemory;
    double memoryIncreasePerOp = (double) memoryIncrease / 1000;

    LOGGER.info("Memory Usage Benchmark Results:");
    LOGGER.info("  Baseline memory: {} KB", baselineMemory / 1024);
    LOGGER.info("  After operations: {} KB", afterOperationsMemory / 1024);
    LOGGER.info("  Memory increase: {} KB", memoryIncrease / 1024);
    LOGGER.info("  Per operation: {:.2f} bytes", memoryIncreasePerOp);

    // Memory increase should be reasonable (less than 1KB per operation)
    assertTrue(memoryIncreasePerOp < 1024, "Memory usage per operation should be reasonable");
  }

  private BenchmarkResult performMethodCalls(int iterations, boolean measureLatency) throws Exception {
    List<Long> latencies = measureLatency ? new ArrayList<>() : null;
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    long startTime = System.nanoTime();

    List<CompletableFuture<Void>> futures = new ArrayList<>();
    
    for (int i = 0; i < iterations; i++) {
      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          long callStart = measureLatency ? System.nanoTime() : 0;
          
          OutboundMethodCall methodCall = createTestMethodCall();
          InboundMessage response = connection.sendRequest(methodCall)
              .toCompletableFuture()
              .get(5, TimeUnit.SECONDS);
          
          assertNotNull(response);
          successCount.incrementAndGet();
          
          if (measureLatency) {
            long callEnd = System.nanoTime();
            synchronized (latencies) {
              latencies.add((callEnd - callStart) / 1_000_000); // Convert to milliseconds
            }
          }
          
        } catch (Exception e) {
          failureCount.incrementAndGet();
        }
      }, executor);
      
      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .get(BENCHMARK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

    long endTime = System.nanoTime();
    double totalTimeMs = (endTime - startTime) / 1_000_000.0;
    double throughput = (successCount.get() * 1000.0) / totalTimeMs;
    double successRate = (double) successCount.get() / iterations;

    if (measureLatency && latencies != null) {
      latencies.sort(Long::compareTo);
      
      double averageLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
      long minLatency = latencies.get(0);
      long maxLatency = latencies.get(latencies.size() - 1);
      long p95Latency = latencies.get((int) (latencies.size() * 0.95));

      return new BenchmarkResult(
          iterations,
          averageLatency,
          minLatency,
          maxLatency,
          p95Latency,
          throughput,
          successRate
      );
    }

    return new BenchmarkResult(iterations, 0, 0, 0, 0, throughput, successRate);
  }

  private OutboundMethodCall createTestMethodCall() {
    return OutboundMethodCall.Builder
        .create()
        .withPath(ObjectPath.valueOf("/org/freedesktop/DBus"))
        .withMember(DBusString.valueOf("GetId"))
        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
        .withReplyExpected(true)
        .build();
  }

  /**
   * Checks if performance tests should be skipped (e.g., when Docker is not available).
   */
  static boolean shouldSkipPerformanceTests() {
    // Skip if running in CI without Docker or if explicitly disabled
    if ("true".equals(System.getProperty("skip.performance.tests")) ||
        "true".equals(System.getenv("SKIP_PERFORMANCE_TESTS"))) {
      return true;
    }
    
    // Skip if D-Bus service is not available on localhost:12345
    try (java.net.Socket socket = new java.net.Socket("localhost", 12345)) {
      return false; // Service is available
    } catch (java.io.IOException e) {
      return true; // Service not available, skip tests
    }
  }

  private static class BenchmarkResult {
    final int totalOperations;
    final double averageLatencyMs;
    final long minLatencyMs;
    final long maxLatencyMs;
    final long p95LatencyMs;
    final double throughputOpsPerSec;
    final double successRate;

    BenchmarkResult(int totalOperations, double averageLatencyMs, long minLatencyMs, 
                   long maxLatencyMs, long p95LatencyMs, double throughputOpsPerSec, double successRate) {
      this.totalOperations = totalOperations;
      this.averageLatencyMs = averageLatencyMs;
      this.minLatencyMs = minLatencyMs;
      this.maxLatencyMs = maxLatencyMs;
      this.p95LatencyMs = p95LatencyMs;
      this.throughputOpsPerSec = throughputOpsPerSec;
      this.successRate = successRate;
    }
  }
}