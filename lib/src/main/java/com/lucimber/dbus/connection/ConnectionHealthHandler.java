/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
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
 * A connection handler that monitors the health of a D-Bus connection by performing periodic ping
 * operations using the standard D-Bus Peer.Ping method.
 *
 * <p>This handler integrates with the connection pipeline and manages connection state transitions,
 * health checks, and event firing to registered listeners.
 *
 * <p>The health monitoring is performed using the standard D-Bus Peer.Ping method, which is
 * supported by all D-Bus implementations. The handler automatically starts monitoring when the
 * connection becomes active and stops when it becomes inactive.
 *
 * <p>Connection state transitions are tracked and notified to registered listeners through the
 * {@link ConnectionEventListener} interface.
 *
 * @see ConnectionState
 * @see ConnectionEvent
 * @see ConnectionEventListener
 * @see ConnectionConfig
 * @since 1.0.0
 */
public final class ConnectionHealthHandler extends AbstractDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHealthHandler.class);

    // Resource limits to prevent exhaustion
    private static final int MAX_PENDING_HEALTH_CHECKS = 100;
    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    private final ConnectionConfig config;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService eventExecutor;
    private final ConcurrentLinkedQueue<ConnectionEventListener> listeners =
            new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<DBusUInt32, CompletableFuture<InboundMessage>>
            pendingHealthChecks = new ConcurrentHashMap<>();

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture<?>> healthCheckFuture = new AtomicReference<>();
    private final AtomicReference<Instant> lastSuccessfulCheck = new AtomicReference<>();
    private final AtomicReference<ConnectionState> currentState =
            new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicReference<Context> contextRef = new AtomicReference<>();
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    /**
     * Creates a new connection health handler with the specified configuration.
     *
     * @param config the connection configuration, must not be null
     * @throws NullPointerException if config is null
     * @since 1.0.0
     */
    public ConnectionHealthHandler(ConnectionConfig config) {
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.scheduler =
                Executors.newScheduledThreadPool(
                        1,
                        r -> {
                            Thread t =
                                    new Thread(
                                            r,
                                            "dbus-health-scheduler-"
                                                    + System.identityHashCode(this));
                            t.setDaemon(true);
                            return t;
                        });
        this.eventExecutor =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t =
                                    new Thread(
                                            r,
                                            "dbus-health-events-" + System.identityHashCode(this));
                            t.setDaemon(true);
                            return t;
                        });
    }

    /**
     * Adds a connection event listener.
     *
     * @param listener the listener to add, ignored if null
     * @since 1.0.0
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a connection event listener.
     *
     * @param listener the listener to remove
     * @since 1.0.0
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Gets the current connection state.
     *
     * @return the current connection state
     * @since 1.0.0
     */
    public ConnectionState getCurrentState() {
        return currentState.get();
    }

    /**
     * Gets the timestamp of the last successful health check.
     *
     * @return the timestamp of the last successful health check, or null if none
     * @since 1.0.0
     */
    public Instant getLastSuccessfulCheck() {
        return lastSuccessfulCheck.get();
    }

    @Override
    public void onHandlerAdded(Context ctx) {
        contextRef.set(ctx);
        super.onHandlerAdded(ctx);
    }

    @Override
    public void onHandlerRemoved(Context ctx) {
        contextRef.set(null);
        super.onHandlerRemoved(ctx);
    }

    @Override
    public void onConnectionActive(Context ctx) {
        LOGGER.debug("Connection became active, starting health monitoring");
        updateState(ConnectionState.CONNECTED);
        startHealthMonitoring(ctx);
        ctx.propagateConnectionActive();
    }

    @Override
    public void onConnectionInactive(Context ctx) {
        LOGGER.debug("Connection became inactive, stopping health monitoring");
        updateState(ConnectionState.DISCONNECTED);
        stopHealthMonitoring();
        ctx.propagateConnectionInactive();
    }

    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        // Check if this is a reply to one of our health check pings
        if (msg instanceof InboundMethodReturn methodReturn) {
            CompletableFuture<InboundMessage> future =
                    pendingHealthChecks.remove(methodReturn.getReplySerial());
            if (future != null) {
                // This is a health check response
                LOGGER.debug(
                        "Received health check response for serial: {}",
                        methodReturn.getReplySerial());
                future.complete(msg);
                return; // Don't propagate health check responses
            }
        }

        // Not a health check response, propagate normally
        ctx.propagateInboundMessage(msg);
    }

    @Override
    public void handleInboundFailure(Context ctx, Throwable cause) {
        LOGGER.warn("Inbound failure detected, updating connection state", cause);

        // Connection failure detected, update state and stop health monitoring
        updateState(ConnectionState.FAILED);
        stopHealthMonitoring();

        // Fail any pending health checks
        pendingHealthChecks.values().forEach(future -> future.completeExceptionally(cause));
        pendingHealthChecks.clear();

        ctx.propagateInboundFailure(cause);
    }

    private void startHealthMonitoring(Context ctx) {
        if (!config.isHealthCheckEnabled()) {
            LOGGER.debug("Health monitoring is disabled in configuration");
            return;
        }

        if (active.compareAndSet(false, true)) {
            LOGGER.info(
                    "Starting connection health monitoring with interval: {}",
                    config.getHealthCheckInterval());
            lastSuccessfulCheck.set(Instant.now());

            ScheduledFuture<?> future =
                    scheduler.scheduleAtFixedRate(
                            () -> performHealthCheck(ctx),
                            config.getHealthCheckInterval().toMillis(),
                            config.getHealthCheckInterval().toMillis(),
                            TimeUnit.MILLISECONDS);

            healthCheckFuture.set(future);
        }
    }

    private void stopHealthMonitoring() {
        if (active.compareAndSet(true, false)) {
            LOGGER.info("Stopping connection health monitoring");

            ScheduledFuture<?> future = healthCheckFuture.getAndSet(null);
            if (future != null) {
                future.cancel(false);
            }

            // Fail any pending health checks
            pendingHealthChecks.values().forEach(f -> f.cancel(true));
            pendingHealthChecks.clear();
        }
    }

    private void performHealthCheck(Context ctx) {
        if (!active.get()) {
            return;
        }

        // Check for too many consecutive failures
        if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            LOGGER.error(
                    "Too many consecutive health check failures ({}), stopping health monitoring",
                    consecutiveFailures.get());
            updateState(ConnectionState.FAILED);
            stopHealthMonitoring();
            return;
        }

        // Check for too many pending health checks
        if (pendingHealthChecks.size() >= MAX_PENDING_HEALTH_CHECKS) {
            LOGGER.warn(
                    "Too many pending health checks ({}), skipping this check",
                    pendingHealthChecks.size());
            return;
        }

        try {
            LOGGER.debug("Performing health check...");

            // Create a ping method call using the standard D-Bus Peer interface
            DBusUInt32 serial = ctx.getConnection().getNextSerial();
            OutboundMethodCall pingCall =
                    OutboundMethodCall.Builder.create()
                            .withSerial(serial)
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("Ping"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
                            .withReplyExpected(true)
                            .withTimeout(config.getHealthCheckTimeout())
                            .build();

            // Set up a future to track the ping response
            CompletableFuture<InboundMessage> pingFuture = new CompletableFuture<>();
            pendingHealthChecks.put(serial, pingFuture);

            // Send the ping message
            CompletableFuture<Void> sendFuture = new CompletableFuture<>();
            sendFuture.exceptionally(
                    throwable -> {
                        pingFuture.completeExceptionally(throwable);
                        return null;
                    });
            ctx.propagateOutboundMessage(pingCall, sendFuture);

            // Handle the ping response asynchronously
            pingFuture
                    .orTimeout(config.getHealthCheckTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .whenComplete(
                            (response, throwable) -> {
                                pendingHealthChecks.remove(serial);

                                if (throwable != null) {
                                    // Health check failed
                                    int failures = consecutiveFailures.incrementAndGet();
                                    LOGGER.warn(
                                            "Health check failed (consecutive failures: {}): {}",
                                            failures,
                                            throwable.getMessage());
                                    fireConnectionEvent(
                                            ConnectionEvent.healthCheckFailure(throwable));

                                    // Update state to unhealthy if we were connected
                                    if (currentState.get() == ConnectionState.CONNECTED) {
                                        updateState(ConnectionState.UNHEALTHY);
                                    }
                                } else {
                                    // Health check succeeded
                                    consecutiveFailures.set(0); // Reset failure counter
                                    lastSuccessfulCheck.set(Instant.now());
                                    fireConnectionEvent(ConnectionEvent.healthCheckSuccess());

                                    // Update state to healthy if we were unhealthy
                                    if (currentState.get() == ConnectionState.UNHEALTHY) {
                                        updateState(ConnectionState.CONNECTED);
                                    }

                                    LOGGER.debug("Health check succeeded");
                                }
                            });

        } catch (Exception e) {
            int failures = consecutiveFailures.incrementAndGet();
            LOGGER.error("Error performing health check (consecutive failures: {})", failures, e);
            fireConnectionEvent(ConnectionEvent.healthCheckFailure(e));

            if (currentState.get() == ConnectionState.CONNECTED) {
                updateState(ConnectionState.UNHEALTHY);
            }
        }
    }

    private void updateState(ConnectionState newState) {
        ConnectionState oldState = currentState.getAndSet(newState);
        if (oldState != newState) {
            LOGGER.debug("Connection state changed from {} to {}", oldState, newState);
            fireConnectionEvent(ConnectionEvent.stateChanged(oldState, newState));
        }
    }

    private void fireConnectionEvent(ConnectionEvent event) {
        if (listeners.isEmpty()) {
            return;
        }

        // Fire events asynchronously to avoid blocking the health check thread
        eventExecutor.execute(
                () -> {
                    Context context = contextRef.get();
                    Connection connection = context != null ? context.getConnection() : null;
                    for (ConnectionEventListener listener : listeners) {
                        try {
                            listener.onConnectionEvent(connection, event);
                        } catch (Exception e) {
                            LOGGER.warn("Error firing connection event to listener", e);
                        }
                    }
                });
    }

    /**
     * Manually triggers a health check.
     *
     * <p>If the handler is not active or no context is available, this method returns a completed
     * future immediately.
     *
     * @return a CompletableFuture that indicates when the health check is triggered
     * @since 1.0.0
     */
    public CompletableFuture<Void> triggerHealthCheck() {
        Context ctx = contextRef.get();
        if (ctx == null || !active.get()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> performHealthCheck(ctx), scheduler);
    }

    /**
     * Shuts down the health monitor and its executors.
     *
     * <p>This method stops all health monitoring, cancels pending health checks, and shuts down the
     * internal thread pools. It should be called when the handler is no longer needed to prevent
     * resource leaks.
     *
     * @since 1.0.0
     */
    public void shutdown() {
        stopHealthMonitoring();

        scheduler.shutdown();
        eventExecutor.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!eventExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                eventExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            eventExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
