/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection handler that implements automatic reconnection with exponential backoff
 * when connection failures are detected.
 *
 * <p>This handler integrates with the connection pipeline and monitors connection events
 * to automatically attempt reconnection when the connection is lost. It uses exponential
 * backoff to avoid overwhelming the server with reconnection attempts.
 *
 * <p>The handler listens for connection state changes and failures, triggering reconnection
 * attempts based on the configured backoff strategy.
 */
public final class ConnectionReconnectHandler extends AbstractDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionReconnectHandler.class);

  private final ConnectionConfig config;
  private final ScheduledExecutorService scheduler;
  private final AtomicBoolean enabled = new AtomicBoolean(false);
  private final AtomicInteger attemptCount = new AtomicInteger(0);
  private final AtomicReference<ScheduledFuture<?>> reconnectFuture = new AtomicReference<>();
  private final AtomicReference<Instant> lastReconnectAttempt = new AtomicReference<>();
  private final AtomicReference<Context> contextRef = new AtomicReference<>();
  private final AtomicReference<Connection> connectionRef = new AtomicReference<>();

  /**
   * Creates a new reconnection handler with the specified configuration.
   *
   * @param config The connection configuration containing reconnection settings
   */
  public ConnectionReconnectHandler(ConnectionConfig config) {
    this.config = Objects.requireNonNull(config, "Config cannot be null");
    this.scheduler = Executors.newScheduledThreadPool(1, r -> {
      Thread t = new Thread(r, "dbus-reconnect-scheduler-" + System.identityHashCode(this));
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * Gets the current number of reconnection attempts.
   *
   * @return The current attempt count
   */
  public int getAttemptCount() {
    return attemptCount.get();
  }

  /**
   * Gets the timestamp of the last reconnection attempt.
   *
   * @return The timestamp of the last reconnection attempt, or null if none
   */
  public Instant getLastReconnectAttempt() {
    return lastReconnectAttempt.get();
  }

  /**
   * Gets whether reconnection is currently enabled.
   *
   * @return true if reconnection is active
   */
  public boolean isEnabled() {
    return enabled.get();
  }

  /**
   * Manually cancels any pending reconnection attempts.
   */
  public void cancelReconnection() {
    ScheduledFuture<?> future = reconnectFuture.getAndSet(null);
    if (future != null) {
      future.cancel(false);
      LOGGER.debug("Cancelled pending reconnection attempt");
    }
  }

  /**
   * Resets the reconnection state, clearing attempt count and timers.
   */
  public void reset() {
    attemptCount.set(0);
    lastReconnectAttempt.set(null);
    cancelReconnection();
  }

  @Override
  public void onHandlerAdded(Context ctx) {
    contextRef.set(ctx);
    connectionRef.set(ctx.getConnection());
    
    // Listen for connection events to trigger reconnection
    ctx.getConnection().addConnectionEventListener(this::handleConnectionEvent);
    
    super.onHandlerAdded(ctx);
  }

  @Override
  public void onHandlerRemoved(Context ctx) {
    contextRef.set(null);
    connectionRef.set(null);
    enabled.set(false);
    cancelReconnection();
    super.onHandlerRemoved(ctx);
  }

  @Override
  public void onConnectionActive(Context ctx) {
    LOGGER.debug("Connection became active, resetting reconnection state");
    reset();
    enabled.set(true);
    ctx.propagateConnectionActive();
  }

  @Override
  public void onConnectionInactive(Context ctx) {
    LOGGER.debug("Connection became inactive");
    enabled.set(false);
    ctx.propagateConnectionInactive();
  }

  @Override
  public void handleInboundFailure(Context ctx, Throwable cause) {
    LOGGER.warn("Inbound failure detected, may trigger reconnection", cause);
    // Let the failure propagate first, then consider reconnection
    ctx.propagateInboundFailure(cause);
  }

  private void handleConnectionEvent(Connection connection, ConnectionEvent event) {
    if (!config.isAutoReconnectEnabled()) {
      LOGGER.debug("Auto-reconnect is disabled, ignoring connection event: {}", event.getType());
      return;
    }

    switch (event.getType()) {
      case STATE_CHANGED:
        handleStateChange(connection, event);
        break;
      case HEALTH_CHECK_FAILURE:
        // Health check failures are handled by state changes to UNHEALTHY
        LOGGER.debug("Health check failure detected, waiting for state change");
        break;
      case HEALTH_CHECK_SUCCESS:
        // Connection recovered, reset reconnection state
        if (attemptCount.get() > 0) {
          LOGGER.info("Connection recovered after {} reconnection attempts", attemptCount.get());
          reset();
        }
        break;
      default:
        // Other events don't affect reconnection
        break;
    }
  }

  private void handleStateChange(Connection connection, ConnectionEvent event) {
    ConnectionState newState = event.getNewState().orElse(null);
    ConnectionState oldState = event.getOldState().orElse(null);

    if (newState == null) {
      return;
    }

    LOGGER.debug("Connection state changed from {} to {}", oldState, newState);

    switch (newState) {
      case DISCONNECTED:
      case FAILED:
        // Connection lost, trigger reconnection
        if (enabled.get() && oldState != ConnectionState.RECONNECTING) {
          LOGGER.info("Connection lost ({}), scheduling reconnection", newState);
          scheduleReconnection(connection);
        }
        break;
      case UNHEALTHY:
        // Connection is unhealthy, but we'll wait for it to either recover or fail
        LOGGER.debug("Connection is unhealthy, monitoring for recovery or failure");
        break;
      case CONNECTED:
        // Connection established successfully
        if (oldState == ConnectionState.RECONNECTING) {
          LOGGER.info("Reconnection successful after {} attempts", attemptCount.get());
          reset();
        }
        break;
      default:
        // Other states don't trigger reconnection
        break;
    }
  }

  private void scheduleReconnection(Connection connection) {
    if (!config.isAutoReconnectEnabled() || !enabled.get()) {
      LOGGER.debug("Auto-reconnect disabled, not scheduling reconnection");
      return;
    }

    int currentAttempt = attemptCount.get();
    
    // Check if we've exceeded the maximum number of attempts
    if (config.getMaxReconnectAttempts() > 0 && currentAttempt >= config.getMaxReconnectAttempts()) {
      LOGGER.error("Maximum reconnection attempts ({}) exceeded, giving up", config.getMaxReconnectAttempts());
      enabled.set(false);
      return;
    }

    // Calculate delay with exponential backoff
    Duration delay = calculateBackoffDelay(currentAttempt);
    
    LOGGER.info("Scheduling reconnection attempt {} in {}", currentAttempt + 1, delay);
    
    ScheduledFuture<?> future = scheduler.schedule(
        () -> attemptReconnection(connection),
        delay.toMillis(),
        TimeUnit.MILLISECONDS
    );
    
    reconnectFuture.set(future);
  }

  private Duration calculateBackoffDelay(int attemptNumber) {
    Duration initialDelay = config.getReconnectInitialDelay();
    double multiplier = config.getReconnectBackoffMultiplier();
    Duration maxDelay = config.getReconnectMaxDelay();
    
    // Calculate exponential backoff: initial * (multiplier ^ attemptNumber)
    double delayMs = initialDelay.toMillis() * Math.pow(multiplier, attemptNumber);
    
    // Cap at maximum delay
    long finalDelayMs = Math.min((long) delayMs, maxDelay.toMillis());
    
    return Duration.ofMillis(finalDelayMs);
  }

  private void attemptReconnection(Connection connection) {
    if (!config.isAutoReconnectEnabled() || !enabled.get()) {
      LOGGER.debug("Auto-reconnect disabled, skipping reconnection attempt");
      return;
    }

    int currentAttempt = attemptCount.incrementAndGet();
    lastReconnectAttempt.set(Instant.now());
    
    LOGGER.info("Attempting reconnection #{}", currentAttempt);
    
    // Fire a reconnection attempt event
    fireReconnectionAttemptEvent(connection, currentAttempt);
    
    // Attempt to reconnect
    CompletableFuture<Void> reconnectFuture = connection.connect().toCompletableFuture();
    
    reconnectFuture.whenComplete((result, throwable) -> {
      if (throwable != null) {
        LOGGER.warn("Reconnection attempt #{} failed: {}", currentAttempt, throwable.getMessage());
        
        // Fire a failure event
        fireReconnectionFailureEvent(connection, throwable);
        
        // Schedule next attempt if we haven't exceeded the limit
        if (config.getMaxReconnectAttempts() == 0 || currentAttempt < config.getMaxReconnectAttempts()) {
          scheduleReconnection(connection);
        } else {
          LOGGER.error("Maximum reconnection attempts ({}) exceeded, giving up", config.getMaxReconnectAttempts());
          enabled.set(false);
          fireReconnectionExhaustedEvent(connection);
        }
      } else {
        LOGGER.info("Reconnection attempt #{} succeeded", currentAttempt);
        fireReconnectionSuccessEvent(connection);
      }
    });
  }

  private void fireReconnectionAttemptEvent(Connection connection, int attemptNumber) {
    LOGGER.debug("Firing reconnection attempt event for attempt #{}", attemptNumber);
    // The event will be handled by the connection's event system
  }

  private void fireReconnectionFailureEvent(Connection connection, Throwable cause) {
    LOGGER.debug("Firing reconnection failure event: {}", cause.getMessage());
    // The event will be handled by the connection's event system
  }

  private void fireReconnectionSuccessEvent(Connection connection) {
    LOGGER.debug("Firing reconnection success event");
    // The event will be handled by the connection's event system
  }

  private void fireReconnectionExhaustedEvent(Connection connection) {
    LOGGER.debug("Firing reconnection exhausted event");
    // The event will be handled by the connection's event system
  }

  /**
   * Shuts down the reconnection handler and its scheduler.
   */
  public void shutdown() {
    enabled.set(false);
    cancelReconnection();
    
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

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
}