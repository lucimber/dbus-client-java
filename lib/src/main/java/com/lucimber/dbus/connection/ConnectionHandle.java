/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.concurrent.CompletionStage;

/**
 * Transport-agnostic handle representing an active D-Bus connection.
 *
 * <p>This interface abstracts the underlying transport implementation and provides a unified way to
 * interact with the connection regardless of whether it's using Netty, NIO, or any other networking
 * framework.
 */
public interface ConnectionHandle {

    /**
     * Checks if the connection is currently active and ready for communication.
     *
     * @return true if the connection is active
     */
    boolean isActive();

    /**
     * Sends a message over this connection.
     *
     * @param message the outbound message to send
     * @return CompletionStage that completes when the message is sent
     */
    CompletionStage<Void> send(OutboundMessage message);

    /**
     * Sends a request message and waits for a response.
     *
     * @param message the outbound request message
     * @return CompletionStage that completes with the response message
     */
    CompletionStage<InboundMessage> sendRequest(OutboundMessage message);

    /**
     * Gets the next available serial number for message sequencing.
     *
     * @return the next serial number
     */
    DBusUInt32 getNextSerial();

    /**
     * Closes this connection handle and releases associated resources.
     *
     * @return CompletionStage that completes when the connection is closed
     */
    CompletionStage<Void> close();

    /**
     * Gets the assigned D-Bus name for this connection, if available.
     *
     * @return the assigned bus name, or null if not yet assigned
     */
    String getAssignedBusName();
}
