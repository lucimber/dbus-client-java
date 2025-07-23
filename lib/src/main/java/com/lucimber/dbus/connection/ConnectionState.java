/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

/**
 * Represents the current state of a D-Bus connection.
 *
 * <p>The connection progresses through these states during its lifecycle:
 *
 * <ul>
 *   <li>{@link #DISCONNECTED} - Initial state, no connection established
 *   <li>{@link #CONNECTING} - Connection attempt in progress
 *   <li>{@link #AUTHENTICATING} - SASL authentication in progress
 *   <li>{@link #CONNECTED} - Connected and operational
 *   <li>{@link #UNHEALTHY} - Connected but health checks are failing
 *   <li>{@link #RECONNECTING} - Attempting to reconnect after failure
 *   <li>{@link #FAILED} - Connection failed and cannot be recovered
 * </ul>
 */
public enum ConnectionState {

    /** No connection is established. This is the initial state. */
    DISCONNECTED,

    /** Connection establishment is in progress (socket connection, channel setup). */
    CONNECTING,

    /** Socket is connected but SASL authentication is in progress. */
    AUTHENTICATING,

    /**
     * Connection is fully established and operational. D-Bus name has been acquired and the
     * connection is ready for use.
     */
    CONNECTED,

    /**
     * Connection exists but health checks are failing. The connection may still work but is
     * considered unreliable.
     */
    UNHEALTHY,

    /** Connection was lost and automatic reconnection is in progress. */
    RECONNECTING,

    /**
     * Connection failed permanently and cannot be recovered. Manual intervention or configuration
     * changes may be required.
     */
    FAILED;

    /**
     * Checks if the connection is in a state where it can handle requests.
     *
     * @return true if the connection can handle D-Bus requests
     */
    public boolean canHandleRequests() {
        return this == CONNECTED || this == UNHEALTHY;
    }

    /**
     * Checks if the connection is attempting to establish or re-establish connectivity.
     *
     * @return true if the connection is in a transitional state
     */
    public boolean isTransitioning() {
        return this == CONNECTING || this == AUTHENTICATING || this == RECONNECTING;
    }

    /**
     * Checks if the connection is in a final failure state.
     *
     * @return true if the connection has failed permanently
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}
