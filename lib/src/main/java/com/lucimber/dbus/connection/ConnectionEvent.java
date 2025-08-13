/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a connection lifecycle event.
 *
 * <p>Connection events are fired when the connection state changes, health checks succeed or fail,
 * or other significant connection-related events occur.
 */
public final class ConnectionEvent {

    private final ConnectionEventType type;
    private final ConnectionState oldState;
    private final ConnectionState newState;
    private final Instant timestamp;
    private final String message;
    private final Throwable cause;

    private ConnectionEvent(Builder builder) {
        this.type = Objects.requireNonNull(builder.type, "Event type cannot be null");
        this.oldState = builder.oldState;
        this.newState = builder.newState;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.message = builder.message;
        this.cause = builder.cause;
    }

    /**
     * Creates a new builder for constructing connection events.
     *
     * @param type The type of event
     * @return A new builder instance
     */
    public static Builder builder(ConnectionEventType type) {
        return new Builder(type);
    }

    /**
     * Creates a state change event.
     *
     * @param oldState The previous connection state
     * @param newState The new connection state
     * @return A state change event
     */
    public static ConnectionEvent stateChanged(ConnectionState oldState, ConnectionState newState) {
        return builder(ConnectionEventType.STATE_CHANGED)
                .withOldState(oldState)
                .withNewState(newState)
                .withMessage("Connection state changed from " + oldState + " to " + newState)
                .build();
    }

    /**
     * Creates a health check success event.
     *
     * @return A health check success event
     */
    public static ConnectionEvent healthCheckSuccess() {
        return builder(ConnectionEventType.HEALTH_CHECK_SUCCESS)
                .withMessage("Health check succeeded")
                .build();
    }

    /**
     * Creates a health check failure event.
     *
     * @param cause The cause of the health check failure
     * @return A health check failure event
     */
    public static ConnectionEvent healthCheckFailure(Throwable cause) {
        return builder(ConnectionEventType.HEALTH_CHECK_FAILURE)
                .withMessage("Health check failed: " + cause.getMessage())
                .withCause(cause)
                .build();
    }

    /**
     * Creates a reconnection attempt event.
     *
     * @param attemptNumber The reconnection attempt number
     * @return A reconnection attempt event
     */
    public static ConnectionEvent reconnectionAttempt(int attemptNumber) {
        return builder(ConnectionEventType.RECONNECTION_ATTEMPT)
                .withMessage("Reconnection attempt #" + attemptNumber)
                .build();
    }

    public ConnectionEventType getType() {
        return type;
    }

    public Optional<ConnectionState> getOldState() {
        return Optional.ofNullable(oldState);
    }

    public Optional<ConnectionState> getNewState() {
        return Optional.ofNullable(newState);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public Optional<Throwable> getCause() {
        return Optional.ofNullable(cause);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ConnectionEvent{");
        sb.append("type=").append(type);
        sb.append(", timestamp=").append(timestamp);
        if (oldState != null || newState != null) {
            sb.append(", state=").append(oldState).append("->").append(newState);
        }
        if (message != null) {
            sb.append(", message='").append(message).append('\'');
        }
        if (cause != null) {
            sb.append(", cause=").append(cause.getClass().getSimpleName());
        }
        sb.append('}');
        return sb.toString();
    }

    /** Builder for creating {@link ConnectionEvent} instances. */
    public static final class Builder {
        private final ConnectionEventType type;
        private ConnectionState oldState;
        private ConnectionState newState;
        private Instant timestamp;
        private String message;
        private Throwable cause;

        private Builder(ConnectionEventType type) {
            this.type = type;
        }

        public Builder withOldState(ConnectionState oldState) {
            this.oldState = oldState;
            return this;
        }

        public Builder withNewState(ConnectionState newState) {
            this.newState = newState;
            return this;
        }

        public Builder withTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder withCause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public ConnectionEvent build() {
            return new ConnectionEvent(this);
        }
    }
}
