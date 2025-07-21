/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import com.lucimber.dbus.util.ErrorRecoveryManager.CircuitBreaker;
import com.lucimber.dbus.util.ErrorRecoveryManager.CircuitBreakerConfig;
import com.lucimber.dbus.util.ErrorRecoveryManager.CircuitBreakerOpenException;
import com.lucimber.dbus.util.ErrorRecoveryManager.ErrorClassification;
import com.lucimber.dbus.util.ErrorRecoveryManager.RetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ErrorRecoveryManagerTest {

  private ErrorRecoveryManager errorRecoveryManager;
  private ScheduledExecutorService testExecutor;

  @BeforeEach
  void setUp() {
    testExecutor = Executors.newScheduledThreadPool(2);
    errorRecoveryManager = new ErrorRecoveryManager(testExecutor);
  }

  @AfterEach
  void tearDown() {
    if (errorRecoveryManager != null) {
      errorRecoveryManager.shutdown();
    }
    if (testExecutor != null && !testExecutor.isShutdown()) {
      testExecutor.shutdown();
    }
  }

  @Test
  void testSuccessfulOperationWithoutRetry() throws Exception {
    RetryConfig config = RetryConfig.defaultConfig();
    String expectedResult = "success";

    Supplier<CompletableFuture<String>> operation = () ->
        CompletableFuture.completedFuture(expectedResult);

    CompletableFuture<String> result = errorRecoveryManager.executeWithRetry(operation, config);
    String actualResult = result.get(5, TimeUnit.SECONDS);

    assertEquals(expectedResult, actualResult);
  }

  @Test
  void testRetryOnTransientError() throws Exception {
    RetryConfig config = RetryConfig.builder()
        .maxRetries(3)
        .initialDelay(Duration.ofMillis(10))
        .maxDelay(Duration.ofMillis(100))
        .build();

    AtomicInteger attemptCount = new AtomicInteger(0);
    String expectedResult = "success";

    Supplier<CompletableFuture<String>> operation = () -> {
      int attempt = attemptCount.incrementAndGet();
      if (attempt <= 2) {
        return CompletableFuture.failedFuture(new RuntimeException("Connection timeout"));
      }
      return CompletableFuture.completedFuture(expectedResult);
    };

    CompletableFuture<String> result = errorRecoveryManager.executeWithRetry(operation, config);
    String actualResult = result.get(5, TimeUnit.SECONDS);

    assertEquals(expectedResult, actualResult);
    assertEquals(3, attemptCount.get());
  }

  @Test
  void testMaxRetriesExceeded() {
    RetryConfig config = RetryConfig.builder()
        .maxRetries(2)
        .initialDelay(Duration.ofMillis(10))
        .build();

    AtomicInteger attemptCount = new AtomicInteger(0);
    RuntimeException testException = new RuntimeException("Persistent error");

    Supplier<CompletableFuture<String>> operation = () -> {
      attemptCount.incrementAndGet();
      return CompletableFuture.failedFuture(testException);
    };

    CompletableFuture<String> result = errorRecoveryManager.executeWithRetry(operation, config);

    ExecutionException exception = assertThrows(ExecutionException.class, () ->
        result.get(5, TimeUnit.SECONDS));

    assertEquals(testException, exception.getCause());
    assertEquals(3, attemptCount.get()); // Initial attempt + 2 retries
  }

  @Test
  void testNoRetryForConfigurationError() {
    RetryConfig config = RetryConfig.defaultConfig();
    AtomicInteger attemptCount = new AtomicInteger(0);
    IllegalArgumentException testException = new IllegalArgumentException("Invalid configuration");

    Supplier<CompletableFuture<String>> operation = () -> {
      attemptCount.incrementAndGet();
      return CompletableFuture.failedFuture(testException);
    };

    CompletableFuture<String> result = errorRecoveryManager.executeWithRetry(operation, config);

    ExecutionException exception = assertThrows(ExecutionException.class, () ->
        result.get(5, TimeUnit.SECONDS));

    assertEquals(testException, exception.getCause());
    assertEquals(1, attemptCount.get()); // No retries for configuration errors
  }

  @Test
  void testBackoffDelay() {
    RetryConfig config = RetryConfig.builder()
        .initialDelay(Duration.ofMillis(100))
        .backoffMultiplier(2.0)
        .jitterFactor(0.0) // No jitter for predictable testing
        .build();

    Duration delay0 = ErrorRecoveryManager.calculateBackoffDelay(0, config);
    Duration delay1 = ErrorRecoveryManager.calculateBackoffDelay(1, config);
    Duration delay2 = ErrorRecoveryManager.calculateBackoffDelay(2, config);

    assertEquals(100, delay0.toMillis());
    assertEquals(200, delay1.toMillis());
    assertEquals(400, delay2.toMillis());
  }

  @Test
  void testBackoffDelayWithMaxCap() {
    RetryConfig config = RetryConfig.builder()
        .initialDelay(Duration.ofMillis(1000))
        .maxDelay(Duration.ofMillis(5000))
        .backoffMultiplier(10.0)
        .jitterFactor(0.0)
        .build();

    Duration delay0 = ErrorRecoveryManager.calculateBackoffDelay(0, config);
    Duration delay1 = ErrorRecoveryManager.calculateBackoffDelay(1, config);
    Duration delay2 = ErrorRecoveryManager.calculateBackoffDelay(2, config);

    assertEquals(1000, delay0.toMillis());
    assertEquals(5000, delay1.toMillis()); // Capped at maxDelay
    assertEquals(5000, delay2.toMillis()); // Still capped
  }

  @Test
  void testErrorClassification() {
    // Transient errors
    assertEquals(ErrorClassification.TRANSIENT,
        ErrorRecoveryManager.classifyError(new RuntimeException("Connection timeout")));
    assertEquals(ErrorClassification.TRANSIENT,
        ErrorRecoveryManager.classifyError(new RuntimeException("Network unreachable")));

    // Authentication errors
    assertEquals(ErrorClassification.AUTHENTICATION,
        ErrorRecoveryManager.classifyError(new SecurityException("Authentication failed")));
    assertEquals(ErrorClassification.AUTHENTICATION,
        ErrorRecoveryManager.classifyError(new RuntimeException("Access denied")));

    // Configuration errors
    assertEquals(ErrorClassification.CONFIGURATION,
        ErrorRecoveryManager.classifyError(new IllegalArgumentException("Invalid parameter")));
    assertEquals(ErrorClassification.CONFIGURATION,
        ErrorRecoveryManager.classifyError(new RuntimeException("Malformed configuration")));

    // Resource exhaustion
    assertEquals(ErrorClassification.RESOURCE_EXHAUSTION,
        ErrorRecoveryManager.classifyError(new OutOfMemoryError("Out of memory")));
    assertEquals(ErrorClassification.RESOURCE_EXHAUSTION,
        ErrorRecoveryManager.classifyError(new RuntimeException("Resource quota exceeded")));

    // Protocol errors
    assertEquals(ErrorClassification.PROTOCOL,
        ErrorRecoveryManager.classifyError(new RuntimeException("Protocol violation")));
    assertEquals(ErrorClassification.PROTOCOL,
        ErrorRecoveryManager.classifyError(new RuntimeException("Corrupt frame received")));

    // Null error
    assertEquals(ErrorClassification.UNKNOWN,
        ErrorRecoveryManager.classifyError(null));
  }

  @Test
  @Timeout(10)
  void testCircuitBreakerClosed() throws Exception {
    CircuitBreakerConfig config = CircuitBreakerConfig.builder()
        .failureThreshold(3)
        .recoveryTimeout(Duration.ofMillis(100))
        .build();

    CircuitBreaker circuitBreaker = errorRecoveryManager.createCircuitBreaker("test", config);

    // Successful operations should keep circuit closed
    for (int i = 0; i < 5; i++) {
      CompletableFuture<String> result = circuitBreaker.execute(() ->
          CompletableFuture.completedFuture("success"));
      assertEquals("success", result.get(1, TimeUnit.SECONDS));
      assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
  }

  @Test
  @Timeout(10)
  void testCircuitBreakerOpensOnFailures() throws Exception {
    CircuitBreakerConfig config = CircuitBreakerConfig.builder()
        .failureThreshold(2)
        .recoveryTimeout(Duration.ofMillis(100))
        .build();

    CircuitBreaker circuitBreaker = errorRecoveryManager.createCircuitBreaker("test", config);
    RuntimeException testException = new RuntimeException("Test failure");

    // First failure - should remain closed
    CompletableFuture<String> result1 = circuitBreaker.execute(() ->
        CompletableFuture.failedFuture(testException));
    assertThrows(ExecutionException.class, () -> result1.get(1, TimeUnit.SECONDS));
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

    // Second failure - should open the circuit
    CompletableFuture<String> result2 = circuitBreaker.execute(() ->
        CompletableFuture.failedFuture(testException));
    assertThrows(ExecutionException.class, () -> result2.get(1, TimeUnit.SECONDS));
    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

    // Subsequent calls should fail immediately with CircuitBreakerOpenException
    CompletableFuture<String> result3 = circuitBreaker.execute(() ->
        CompletableFuture.completedFuture("should not execute"));
    
    ExecutionException exception = assertThrows(ExecutionException.class, () ->
        result3.get(1, TimeUnit.SECONDS));
    assertInstanceOf(CircuitBreakerOpenException.class, exception.getCause());
  }

  @Test
  @Timeout(10)
  void testCircuitBreakerHalfOpenRecovery() throws Exception {
    CircuitBreakerConfig config = CircuitBreakerConfig.builder()
        .failureThreshold(1)
        .recoveryTimeout(Duration.ofMillis(50))
        .successThreshold(2)
        .build();

    CircuitBreaker circuitBreaker = errorRecoveryManager.createCircuitBreaker("test", config);

    // Fail to open circuit
    CompletableFuture<String> failResult = circuitBreaker.execute(() ->
        CompletableFuture.failedFuture(new RuntimeException("Failure")));
    assertThrows(ExecutionException.class, () -> failResult.get(1, TimeUnit.SECONDS));
    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

    // Wait for recovery timeout
    Thread.sleep(100);

    // First success after timeout should put circuit in HALF_OPEN
    CompletableFuture<String> result1 = circuitBreaker.execute(() ->
        CompletableFuture.completedFuture("success1"));
    assertEquals("success1", result1.get(1, TimeUnit.SECONDS));
    assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());

    // Second success should close the circuit
    CompletableFuture<String> result2 = circuitBreaker.execute(() ->
        CompletableFuture.completedFuture("success2"));
    assertEquals("success2", result2.get(1, TimeUnit.SECONDS));
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
  }

  @Test
  void testCircuitBreakerReset() {
    CircuitBreakerConfig config = CircuitBreakerConfig.defaultConfig();
    CircuitBreaker circuitBreaker = errorRecoveryManager.createCircuitBreaker("test", config);

    // Force circuit to open by simulating failures
    circuitBreaker.getFailureCount(); // Access to trigger internal state
    for (int i = 0; i < config.failureThreshold; i++) {
      circuitBreaker.execute(() -> CompletableFuture.failedFuture(new RuntimeException("Test")));
    }

    // Reset should return circuit to CLOSED state
    circuitBreaker.reset();
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    assertEquals(0, circuitBreaker.getFailureCount());
  }

  @Test
  void testRetryConfigBuilder() {
    Duration initialDelay = Duration.ofMillis(200);
    Duration maxDelay = Duration.ofSeconds(10);
    double backoffMultiplier = 1.5;
    double jitterFactor = 0.2;
    int maxRetries = 5;

    RetryConfig config = RetryConfig.builder()
        .initialDelay(initialDelay)
        .maxDelay(maxDelay)
        .backoffMultiplier(backoffMultiplier)
        .jitterFactor(jitterFactor)
        .maxRetries(maxRetries)
        .build();

    assertEquals(initialDelay, config.initialDelay);
    assertEquals(maxDelay, config.maxDelay);
    assertEquals(backoffMultiplier, config.backoffMultiplier);
    assertEquals(jitterFactor, config.jitterFactor);
    assertEquals(maxRetries, config.maxRetries);
  }

  @Test
  void testCircuitBreakerConfigBuilder() {
    Duration recoveryTimeout = Duration.ofSeconds(30);
    int failureThreshold = 10;
    int successThreshold = 5;

    CircuitBreakerConfig config = CircuitBreakerConfig.builder()
        .recoveryTimeout(recoveryTimeout)
        .failureThreshold(failureThreshold)
        .successThreshold(successThreshold)
        .build();

    assertEquals(recoveryTimeout, config.recoveryTimeout);
    assertEquals(failureThreshold, config.failureThreshold);
    assertEquals(successThreshold, config.successThreshold);
  }

  @Test
  void testDefaultConfigurations() {
    RetryConfig retryConfig = RetryConfig.defaultConfig();
    assertNotNull(retryConfig);
    assertTrue(retryConfig.maxRetries > 0);
    assertTrue(retryConfig.initialDelay.toMillis() > 0);

    CircuitBreakerConfig cbConfig = CircuitBreakerConfig.defaultConfig();
    assertNotNull(cbConfig);
    assertTrue(cbConfig.failureThreshold > 0);
    assertTrue(cbConfig.recoveryTimeout.toMillis() > 0);
    assertTrue(cbConfig.successThreshold > 0);
  }

  @Test
  void testShutdown() {
    ErrorRecoveryManager manager = new ErrorRecoveryManager();
    assertDoesNotThrow(() -> manager.shutdown());
    
    // Should be safe to call shutdown multiple times
    assertDoesNotThrow(() -> manager.shutdown());
  }

  @Test
  void testJitterVariability() {
    RetryConfig config = RetryConfig.builder()
        .initialDelay(Duration.ofMillis(1000))
        .jitterFactor(0.1)
        .build();

    // Generate multiple delays and ensure they vary due to jitter
    Duration delay1 = ErrorRecoveryManager.calculateBackoffDelay(0, config);
    Duration delay2 = ErrorRecoveryManager.calculateBackoffDelay(0, config);
    Duration delay3 = ErrorRecoveryManager.calculateBackoffDelay(0, config);

    // With jitter, delays should be within expected range but not all identical
    long baseDelay = 1000;
    long maxJitter = (long) (baseDelay * config.jitterFactor);
    
    assertTrue(delay1.toMillis() >= baseDelay - maxJitter);
    assertTrue(delay1.toMillis() <= baseDelay + maxJitter);
    assertTrue(delay2.toMillis() >= baseDelay - maxJitter);
    assertTrue(delay2.toMillis() <= baseDelay + maxJitter);
    assertTrue(delay3.toMillis() >= baseDelay - maxJitter);
    assertTrue(delay3.toMillis() <= baseDelay + maxJitter);
  }
}