/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection;

/**
 * Listener interface for connection lifecycle events.
 *
 * <p>Implementations can register with a connection to receive notifications about state changes,
 * health check results, and reconnection attempts.
 */
@FunctionalInterface
public interface ConnectionEventListener {

    /**
     * Called when a connection event occurs.
     *
     * @param connection The connection that fired the event
     * @param event The event that occurred
     */
    void onConnectionEvent(Connection connection, ConnectionEvent event);
}
