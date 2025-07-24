/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.concurrent.CompletionStage;

/**
 * Represents a virtual channel for communication over D-Bus.
 *
 * <p>A D-Bus connection enables inter-process communication by allowing messages to be sent and
 * received. There are two primary types of buses in D-Bus:
 *
 * <ul>
 *   <li><strong>System Bus</strong>: A bus shared between system and user processes.
 *   <li><strong>Session Bus</strong>: A user-specific bus available within a single login session.
 * </ul>
 *
 * Applications can also establish their own session buses for isolated communication within a
 * session.
 */
public interface Connection extends AutoCloseable {

    /**
     * Initiates a connection to a D-Bus instance.
     *
     * @return a {@link CompletionStage} that completes when the connection is established, or
     *     exceptionally if the attempt fails.
     */
    CompletionStage<Void> connect();

    /**
     * Indicates whether the connection has been successfully established and is active.
     *
     * @return {@code true} if the connection is active, {@code false} otherwise.
     */
    boolean isConnected();

    /**
     * Retrieves the associated {@link Pipeline} for this connection.
     *
     * @return the {@link Pipeline} instance used by this connection.
     */
    Pipeline getPipeline();

    /**
     * Generates and returns the next unique serial number for outbound messages.
     *
     * <p>Serial numbers are used to correlate requests and replies and are unique per connection.
     *
     * @return a unique {@link DBusUInt32} serial number for an {@link OutboundMessage}.
     */
    DBusUInt32 getNextSerial();

    /**
     * Sends the given {@link OutboundMessage} over this connection, bypassing the pipeline.
     *
     * <p>This method is intended for simple request-response interactions where no additional
     * pipeline-based processing is needed. The returned {@link CompletionStage} is completed
     * directly with the corresponding {@link InboundMessage} response or exceptionally if an error
     * occurs.
     *
     * <p><b>Note:</b> Use this method only for straightforward communication scenarios that do not
     * require handler involvement or advanced message routing.
     *
     * @param msg the outbound message to send.
     * @return a {@link CompletionStage} that completes with the corresponding inbound response
     *     message, or fails exceptionally on error.
     */
    CompletionStage<InboundMessage> sendRequest(OutboundMessage msg);

    /**
     * Sends the given {@link OutboundMessage} over the connection and completes the provided future
     * when the message has been written to the D-Bus transport.
     *
     * <p>The outbound message is transmitted directly over the connection and does
     * <strong>not</strong> pass through the outbound pipeline. However, the corresponding {@link
     * InboundMessage} response will be delivered through the pipeline, allowing it to be processed
     * by registered {@link InboundHandler}s.
     *
     * <p>This method is intended for scenarios where custom or advanced processing of responses is
     * needed, while keeping message transmission efficient.
     *
     * @param msg the outbound message to send.
     * @param future the {@link CompletionStage} to complete once the message is written or if an
     *     error occurs.
     */
    void sendAndRouteResponse(OutboundMessage msg, CompletionStage<Void> future);

    /**
     * Retrieves the configuration for this connection.
     *
     * @return the {@link ConnectionConfig} instance used by this connection.
     */
    ConnectionConfig getConfig();

    /**
     * Gets the current connection state.
     *
     * @return the current {@link ConnectionState}
     */
    ConnectionState getState();

    /**
     * Adds a connection event listener to receive notifications about connection events.
     *
     * @param listener the listener to add
     */
    void addConnectionEventListener(ConnectionEventListener listener);

    /**
     * Removes a connection event listener.
     *
     * @param listener the listener to remove
     */
    void removeConnectionEventListener(ConnectionEventListener listener);

    /**
     * Manually triggers a health check if health monitoring is enabled.
     *
     * @return a {@link CompletionStage} that completes when the health check is triggered
     */
    CompletionStage<Void> triggerHealthCheck();

    /**
     * Gets the current number of reconnection attempts.
     *
     * @return the current reconnection attempt count
     */
    int getReconnectAttemptCount();

    /** Cancels any pending reconnection attempts. */
    void cancelReconnection();

    /** Resets the reconnection state, clearing attempt count and timers. */
    void resetReconnectionState();
}
