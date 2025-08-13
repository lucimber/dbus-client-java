/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.net.SocketAddress;
import java.util.concurrent.CompletionStage;

/**
 * Strategy interface for different D-Bus connection transport types.
 *
 * <p>This interface defines the strategy pattern for handling different transport mechanisms (Unix
 * domain sockets, TCP, etc.) in an implementation-agnostic way. The strategy encapsulates
 * transport-specific connection logic while remaining independent of any particular networking
 * framework.
 */
public interface ConnectionStrategy {

    /**
     * Checks if this strategy can handle the given socket address.
     *
     * @param address the socket address to check
     * @return true if this strategy supports the address type
     */
    boolean supports(SocketAddress address);

    /**
     * Establishes a connection using this transport strategy.
     *
     * @param address the socket address to connect to
     * @param config connection configuration
     * @param context connection context providing callbacks and handlers
     * @return CompletionStage that completes when connection is established
     */
    CompletionStage<ConnectionHandle> connect(
            SocketAddress address, ConnectionConfig config, ConnectionContext context);

    /**
     * Gets a human-readable name for this transport strategy.
     *
     * @return transport name for logging
     */
    String getTransportName();

    /**
     * Checks if the required transport capabilities are available on this platform.
     *
     * @return true if this transport can be used on the current platform
     */
    boolean isAvailable();
}
