/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.util.ErrorRecoveryManager;
import com.lucimber.dbus.util.ErrorRecoveryManager.CircuitBreaker;
import com.lucimber.dbus.util.ErrorRecoveryManager.CircuitBreakerConfig;

/**
 * Manages the lifecycle of a connection including connect, disconnect, and state transitions. This
 * class encapsulates connection state management and ensures thread-safe state transitions.
 */
public class ConnectionLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLifecycleManager.class);

    private final ConnectionConfig config;
    private final AtomicReference<ConnectionState> state;
    private final AtomicBoolean connecting;
    private final AtomicBoolean closing;
    private final ErrorRecoveryManager errorRecoveryManager;
    private final CircuitBreaker connectionCircuitBreaker;
    private final String connectionId;

    /**
     * Creates a new connection lifecycle manager.
     *
     * @param config the connection configuration
     * @param connectionId unique identifier for this connection
     */
    public ConnectionLifecycleManager(ConnectionConfig config, String connectionId) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.connectionId = Objects.requireNonNull(connectionId, "connectionId must not be null");
        this.state = new AtomicReference<>(ConnectionState.DISCONNECTED);
        this.connecting = new AtomicBoolean(false);
        this.closing = new AtomicBoolean(false);

        // Initialize error recovery manager
        this.errorRecoveryManager = new ErrorRecoveryManager();

        // Create circuit breaker for connection operations
        CircuitBreakerConfig cbConfig =
                CircuitBreakerConfig.builder()
                        .failureThreshold(3)
                        .recoveryTimeout(config.getConnectTimeout().multipliedBy(2))
                        .successThreshold(2)
                        .build();
        this.connectionCircuitBreaker =
                errorRecoveryManager.createCircuitBreaker("connection-" + connectionId, cbConfig);
    }

    /**
     * Initiates a connection attempt.
     *
     * @param connectionSupplier supplier that performs the actual connection
     * @return completion stage that completes when connection is established
     */
    public CompletionStage<ConnectionHandle> connect(ConnectionSupplier connectionSupplier) {

        // Check if already connecting or connected
        if (!connecting.compareAndSet(false, true)) {
            return CompletableFuture.failedStage(
                    new IllegalStateException("Connection attempt already in progress"));
        }

        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTED) {
            connecting.set(false);
            return CompletableFuture.failedStage(new IllegalStateException("Already connected"));
        }

        if (closing.get()) {
            connecting.set(false);
            return CompletableFuture.failedStage(
                    new IllegalStateException("Connection is closing"));
        }

        LOGGER.info("Initiating connection for {}", connectionId);
        updateState(ConnectionState.CONNECTING);

        // Use circuit breaker for connection attempt
        return connectionCircuitBreaker.execute(
                () ->
                        connectionSupplier
                                .connect()
                                .toCompletableFuture()
                                .whenComplete(
                                        (handle, error) -> {
                                            connecting.set(false);
                                            if (error != null) {
                                                LOGGER.error(
                                                        "Connection failed for {}",
                                                        connectionId,
                                                        error);
                                                updateState(ConnectionState.FAILED);
                                            } else {
                                                LOGGER.info(
                                                        "Connection established for {}",
                                                        connectionId);
                                                updateState(ConnectionState.CONNECTED);
                                            }
                                        }));
    }

    /**
     * Initiates disconnection.
     *
     * @param disconnectionSupplier supplier that performs the actual disconnection
     * @return completion stage that completes when disconnection is done
     */
    public CompletionStage<Void> disconnect(DisconnectionSupplier disconnectionSupplier) {
        if (!closing.compareAndSet(false, true)) {
            return CompletableFuture.completedStage(null);
        }

        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.DISCONNECTED) {
            return CompletableFuture.completedStage(null);
        }

        LOGGER.info("Initiating disconnection for {}", connectionId);
        // No DISCONNECTING state, so we keep current state

        return disconnectionSupplier
                .disconnect()
                .handle(
                        (result, error) -> {
                            if (error != null) {
                                LOGGER.warn(
                                        "Error during disconnection for {}", connectionId, error);
                            }
                            updateState(ConnectionState.DISCONNECTED);
                            closing.set(false);
                            return result;
                        });
    }

    /**
     * Gets the current connection state.
     *
     * @return current connection state
     */
    public ConnectionState getState() {
        return state.get();
    }

    /**
     * Checks if the connection is currently connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    /**
     * Checks if a connection attempt is in progress.
     *
     * @return true if connecting, false otherwise
     */
    public boolean isConnecting() {
        return connecting.get();
    }

    /**
     * Checks if disconnection is in progress.
     *
     * @return true if closing, false otherwise
     */
    public boolean isClosing() {
        return closing.get();
    }

    /**
     * Updates the connection state and notifies listeners.
     *
     * @param newState the new connection state
     */
    private void updateState(ConnectionState newState) {
        ConnectionState oldState = state.getAndSet(newState);
        if (oldState != newState) {
            LOGGER.debug(
                    "Connection {} state changed from {} to {}", connectionId, oldState, newState);
        }
    }

    /**
     * Gets the error recovery manager.
     *
     * @return error recovery manager
     */
    public ErrorRecoveryManager getErrorRecoveryManager() {
        return errorRecoveryManager;
    }

    /**
     * Gets the connection circuit breaker.
     *
     * @return circuit breaker for connection operations
     */
    public CircuitBreaker getConnectionCircuitBreaker() {
        return connectionCircuitBreaker;
    }

    /** Functional interface for connection operations. */
    @FunctionalInterface
    public interface ConnectionSupplier {
        /**
         * Performs the connection operation.
         *
         * @return completion stage with connection handle
         */
        CompletionStage<ConnectionHandle> connect();
    }

    /** Functional interface for disconnection operations. */
    @FunctionalInterface
    public interface DisconnectionSupplier {
        /**
         * Performs the disconnection operation.
         *
         * @return completion stage that completes when disconnected
         */
        CompletionStage<Void> disconnect();
    }
}
