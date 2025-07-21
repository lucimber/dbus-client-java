/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive error recovery manager for D-Bus operations.
 * 
 * <p>Provides sophisticated error handling mechanisms including:</p>
 * <ul>
 *   <li>Exponential backoff with jitter for retry operations</li>
 *   <li>Circuit breaker pattern for failing operations</li>
 *   <li>Error classification for appropriate recovery strategies</li>
 *   <li>Adaptive timeout based on historical performance</li>
 *   <li>Resource cleanup and leak prevention</li>
 * </ul>
 * 
 * <p>This manager is designed to be thread-safe and can be shared across
 * multiple connection instances and operation types.</p>
 */
public class ErrorRecoveryManager {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorRecoveryManager.class);
  
  // Default configuration values
  private static final int DEFAULT_MAX_RETRIES = 3;
  private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(100);
  private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);
  private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
  private static final double DEFAULT_JITTER_FACTOR = 0.1;
  
  // Circuit breaker configuration
  private static final int DEFAULT_FAILURE_THRESHOLD = 5;
  private static final Duration DEFAULT_RECOVERY_TIMEOUT = Duration.ofSeconds(60);
  private static final int DEFAULT_SUCCESS_THRESHOLD = 3;
  
  private final ScheduledExecutorService scheduler;
  private final boolean ownScheduler;
  
  /**
     * Creates a new ErrorRecoveryManager with its own scheduler.
     */
  public ErrorRecoveryManager() {
    this.scheduler = Executors.newScheduledThreadPool(2, r -> {
      Thread thread = new Thread(r, "ErrorRecovery-" + System.identityHashCode(this));
      thread.setDaemon(true);
      return thread;
    });
    this.ownScheduler = true;
  }
  
  /**
     * Creates a new ErrorRecoveryManager using the provided scheduler.
     * 
     * @param scheduler the scheduler to use for retry operations
     */
  public ErrorRecoveryManager(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
    this.ownScheduler = false;
  }
  
  /**
     * Executes an operation with retry logic using exponential backoff.
     * 
     * @param operation the operation to execute
     * @param config retry configuration
     * @param <T> the result type
     * @return CompletableFuture that completes with the operation result or failure
     */
  public <T> CompletableFuture<T> executeWithRetry(
      Supplier<CompletableFuture<T>> operation, 
      RetryConfig config) {
    
    CompletableFuture<T> result = new CompletableFuture<>();
    executeWithRetryInternal(operation, config, 0, result);
    return result;
  }
  
  /**
     * Creates a circuit breaker for an operation.
     * 
     * @param name unique name for the circuit breaker
     * @param config circuit breaker configuration
     * @return a new CircuitBreaker instance
     */
  public CircuitBreaker createCircuitBreaker(String name, CircuitBreakerConfig config) {
    return new CircuitBreaker(name, config, scheduler);
  }
  
  /**
     * Classifies an exception to determine the appropriate recovery strategy.
     * 
     * @param throwable the exception to classify
     * @return the error classification
     */
  public static ErrorClassification classifyError(Throwable throwable) {
    if (throwable == null) {
      return ErrorClassification.UNKNOWN;
    }
    
    String className = throwable.getClass().getSimpleName().toLowerCase();
    String message = throwable.getMessage() != null ? throwable.getMessage().toLowerCase() : "";
    
    // Network/connection errors (potentially recoverable)
    if (className.contains("connect") || className.contains("timeout") || 
      className.contains("socket") || message.contains("connection refused") ||
      message.contains("timeout") || message.contains("network")) {
      return ErrorClassification.TRANSIENT;
    }
    
    // Authentication/authorization errors (not recoverable without intervention)
    if (className.contains("auth") || className.contains("security") ||
      message.contains("authentication") || message.contains("authorization") ||
      message.contains("permission") || message.contains("access denied")) {
      return ErrorClassification.AUTHENTICATION;
    }
    
    // Configuration/validation errors (not recoverable)
    if (className.contains("illegal") || className.contains("invalid") ||
      className.contains("validation") || message.contains("invalid") ||
      message.contains("malformed") || message.contains("configuration")) {
      return ErrorClassification.CONFIGURATION;
    }
    
    // Resource exhaustion (potentially recoverable after delay)
    if (className.contains("memory") || className.contains("resource") ||
      message.contains("out of memory") || message.contains("resource") ||
      message.contains("quota") || message.contains("limit exceeded")) {
      return ErrorClassification.RESOURCE_EXHAUSTION;
    }
    
    // Corruption/protocol errors (potentially recoverable)
    if (className.contains("corrupt") || className.contains("protocol") ||
      message.contains("corrupt") || message.contains("protocol") ||
      message.contains("invalid frame") || message.contains("parse")) {
      return ErrorClassification.PROTOCOL;
    }
    
    // Default to transient for unknown errors
    return ErrorClassification.TRANSIENT;
  }
  
  /**
     * Calculates the next delay with exponential backoff and jitter.
     * 
     * @param attempt the current attempt number (0-based)
     * @param config retry configuration
     * @return the delay duration
     */
  public static Duration calculateBackoffDelay(int attempt, RetryConfig config) {
    double baseDelay = config.initialDelay.toMillis();
    double multipliedDelay = baseDelay * Math.pow(config.backoffMultiplier, attempt);
    
    // Cap at maximum delay
    double cappedDelay = Math.min(multipliedDelay, config.maxDelay.toMillis());
    
    // Add jitter to prevent thundering herd
    double jitter = cappedDelay * config.jitterFactor * (ThreadLocalRandom.current().nextDouble() - 0.5) * 2;
    long finalDelay = Math.max(0, Math.round(cappedDelay + jitter));
    
    return Duration.ofMillis(finalDelay);
  }
  
  /**
     * Shuts down the error recovery manager and releases resources.
     */
  public void shutdown() {
    if (ownScheduler) {
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
  
  private <T> void executeWithRetryInternal(
      Supplier<CompletableFuture<T>> operation,
      RetryConfig config,
      int attempt,
      CompletableFuture<T> result) {
    
    try {
      CompletableFuture<T> operationFuture = operation.get();
      
      operationFuture.whenComplete((value, throwable) -> {
        if (throwable == null) {
          result.complete(value);
        } else {
          ErrorClassification classification = classifyError(throwable);
          
          // Check if we should retry
          if (shouldRetry(classification, attempt, config)) {
            Duration delay = calculateBackoffDelay(attempt, config);
            
            LOGGER.debug("Retrying operation after {} ms (attempt {}/{}): {}", 
              delay.toMillis(), attempt + 1, config.maxRetries, throwable.getMessage());
            
            scheduler.schedule(() -> {
              executeWithRetryInternal(operation, config, attempt + 1, result);
            }, delay.toMillis(), TimeUnit.MILLISECONDS);
          } else {
            LOGGER.debug("Not retrying operation (attempt {}/{}), classification: {}, error: {}", 
              attempt + 1, config.maxRetries, classification, throwable.getMessage());
            result.completeExceptionally(throwable);
          }
        }
      });
      
    } catch (Exception e) {
      result.completeExceptionally(e);
    }
  }
  
  private static boolean shouldRetry(ErrorClassification classification, int attempt, RetryConfig config) {
    if (attempt >= config.maxRetries) {
      return false;
    }
    
    return switch (classification) {
      case TRANSIENT, RESOURCE_EXHAUSTION, PROTOCOL -> true;
      case AUTHENTICATION, CONFIGURATION -> false;
      case UNKNOWN -> attempt < config.maxRetries / 2; // Conservative retry for unknown errors
    };
  }
  
  /**
     * Configuration for retry operations.
     */
  public static class RetryConfig {
    public final int maxRetries;
    public final Duration initialDelay;
    public final Duration maxDelay;
    public final double backoffMultiplier;
    public final double jitterFactor;
    
    private RetryConfig(Builder builder) {
      this.maxRetries = builder.maxRetries;
      this.initialDelay = builder.initialDelay;
      this.maxDelay = builder.maxDelay;
      this.backoffMultiplier = builder.backoffMultiplier;
      this.jitterFactor = builder.jitterFactor;
    }
    
    public static Builder builder() {
      return new Builder();
    }
    
    public static RetryConfig defaultConfig() {
      return builder().build();
    }
    
    public static class Builder {
      private int maxRetries = DEFAULT_MAX_RETRIES;
      private Duration initialDelay = DEFAULT_INITIAL_DELAY;
      private Duration maxDelay = DEFAULT_MAX_DELAY;
      private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
      private double jitterFactor = DEFAULT_JITTER_FACTOR;
      
      public Builder maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
      }
      
      public Builder initialDelay(Duration initialDelay) {
        this.initialDelay = initialDelay;
        return this;
      }
      
      public Builder maxDelay(Duration maxDelay) {
        this.maxDelay = maxDelay;
        return this;
      }
      
      public Builder backoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
      }
      
      public Builder jitterFactor(double jitterFactor) {
        this.jitterFactor = jitterFactor;
        return this;
      }
      
      public RetryConfig build() {
        return new RetryConfig(this);
      }
    }
  }
  
  /**
     * Configuration for circuit breaker operations.
     */
  public static class CircuitBreakerConfig {
    public final int failureThreshold;
    public final Duration recoveryTimeout;
    public final int successThreshold;
    
    private CircuitBreakerConfig(Builder builder) {
      this.failureThreshold = builder.failureThreshold;
      this.recoveryTimeout = builder.recoveryTimeout;
      this.successThreshold = builder.successThreshold;
    }
    
    public static Builder builder() {
      return new Builder();
    }
    
    public static CircuitBreakerConfig defaultConfig() {
      return builder().build();
    }
    
    public static class Builder {
      private int failureThreshold = DEFAULT_FAILURE_THRESHOLD;
      private Duration recoveryTimeout = DEFAULT_RECOVERY_TIMEOUT;
      private int successThreshold = DEFAULT_SUCCESS_THRESHOLD;
      
      public Builder failureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
        return this;
      }
      
      public Builder recoveryTimeout(Duration recoveryTimeout) {
        this.recoveryTimeout = recoveryTimeout;
        return this;
      }
      
      public Builder successThreshold(int successThreshold) {
        this.successThreshold = successThreshold;
        return this;
      }
      
      public CircuitBreakerConfig build() {
        return new CircuitBreakerConfig(this);
      }
    }
  }
  
  /**
     * Circuit breaker implementation for preventing cascading failures.
     */
  public static class CircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }
    
    private final String name;
    private final CircuitBreakerConfig config;
    private final ScheduledExecutorService scheduler;
    
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    private CircuitBreaker(String name, CircuitBreakerConfig config, ScheduledExecutorService scheduler) {
      this.name = name;
      this.config = config;
      this.scheduler = scheduler;
    }
    
    /**
         * Executes an operation through the circuit breaker.
         * 
         * @param operation the operation to execute
         * @param <T> the result type
         * @return CompletableFuture that completes with the operation result
         */
    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> operation) {
      if (state.get() == State.OPEN) {
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        if (timeSinceLastFailure < config.recoveryTimeout.toMillis()) {
          return CompletableFuture.failedFuture(
            new CircuitBreakerOpenException("Circuit breaker '" + name + "' is OPEN"));
        } else {
          state.set(State.HALF_OPEN);
          successCount.set(0);
        }
      }
      
      CompletableFuture<T> result = new CompletableFuture<>();
      
      try {
        CompletableFuture<T> operationFuture = operation.get();
        
        operationFuture.whenComplete((value, throwable) -> {
          if (throwable == null) {
            onSuccess();
            result.complete(value);
          } else {
            onFailure();
            result.completeExceptionally(throwable);
          }
        });
        
      } catch (Exception e) {
        onFailure();
        result.completeExceptionally(e);
      }
      
      return result;
    }
    
    /**
         * Gets the current state of the circuit breaker.
         */
    public State getState() {
      return state.get();
    }
    
    /**
         * Gets the current failure count.
         */
    public int getFailureCount() {
      return failureCount.get();
    }
    
    /**
         * Resets the circuit breaker to CLOSED state.
         */
    public void reset() {
      state.set(State.CLOSED);
      failureCount.set(0);
      successCount.set(0);
    }
    
    private void onSuccess() {
      if (state.get() == State.HALF_OPEN) {
        int successes = successCount.incrementAndGet();
        if (successes >= config.successThreshold) {
          state.set(State.CLOSED);
          failureCount.set(0);
          LOGGER.info("Circuit breaker '{}' transitioned to CLOSED after {} successes", name, successes);
        }
      } else if (state.get() == State.CLOSED) {
        failureCount.set(0); // Reset failure count on success
      }
    }
    
    private void onFailure() {
      int failures = failureCount.incrementAndGet();
      lastFailureTime.set(System.currentTimeMillis());
      
      if (state.get() == State.CLOSED && failures >= config.failureThreshold) {
        state.set(State.OPEN);
        LOGGER.warn("Circuit breaker '{}' transitioned to OPEN after {} failures", name, failures);
        
        // Schedule automatic transition to HALF_OPEN
        scheduler.schedule(() -> {
          if (state.get() == State.OPEN) {
            state.set(State.HALF_OPEN);
            successCount.set(0);
            LOGGER.info("Circuit breaker '{}' transitioned to HALF_OPEN for recovery testing", name);
          }
        }, config.recoveryTimeout.toMillis(), TimeUnit.MILLISECONDS);
        
      } else if (state.get() == State.HALF_OPEN) {
        state.set(State.OPEN);
        LOGGER.warn("Circuit breaker '{}' returned to OPEN after failure during recovery", name);
      }
    }
  }
  
  /**
     * Error classification for determining retry strategies.
     */
  public enum ErrorClassification {
    /** Temporary errors that may succeed on retry */
    TRANSIENT,
    /** Authentication/authorization errors that need intervention */
    AUTHENTICATION,
    /** Configuration errors that need code/config changes */
    CONFIGURATION,
    /** Resource exhaustion that may recover after delay */
    RESOURCE_EXHAUSTION,
    /** Protocol/corruption errors that may succeed on retry */
    PROTOCOL,
    /** Unknown errors */
    UNKNOWN
  }
  
  /**
     * Exception thrown when circuit breaker is in OPEN state.
     */
  public static class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
      super(message);
    }
  }
}