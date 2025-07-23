/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;

/**
 * Context interface providing callbacks and handlers for connection strategies.
 *
 * <p>This interface allows connection strategies to interact with the broader connection management
 * system in a decoupled way, without depending on specific implementation details.
 */
public interface ConnectionContext {

    /**
     * Called when a message is received from the transport.
     *
     * @param message the received inbound message
     */
    void onMessageReceived(InboundMessage message);

    /**
     * Called when the connection state changes.
     *
     * @param newState the new connection state
     */
    void onStateChanged(ConnectionState newState);

    /**
     * Called when a connection error occurs.
     *
     * @param error the error that occurred
     */
    void onError(Throwable error);

    /** Called when the connection is successfully established and authenticated. */
    void onConnectionEstablished();

    /** Called when the connection is lost or closed. */
    void onConnectionLost();

    /**
     * Gets the pipeline for this connection context.
     *
     * @return the connection pipeline
     */
    Pipeline getPipeline();
}
