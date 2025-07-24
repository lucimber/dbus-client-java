/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection;

/**
 * Represents an ordered chain of {@link Handler} instances associated with a {@link Connection}.
 *
 * <p>A {@code Pipeline} manages the flow of inbound and outbound events through the handler chain.
 * Handlers can be dynamically added or removed, and events can be propagated through the pipeline.
 *
 * @see Handler
 * @see Connection
 * @see InboundPropagator
 * @see OutboundPropagator
 */
public interface Pipeline extends InboundPropagator, OutboundPropagator {

    /**
     * Appends a new {@link Handler} to the end of the pipeline.
     *
     * @param name a unique name used to identify the handler within the pipeline.
     * @param handler the {@link Handler} instance to add.
     * @return this {@code Pipeline} instance for method chaining.
     * @throws IllegalArgumentException if a handler with the specified name already exists.
     */
    Pipeline addLast(String name, Handler handler);

    /**
     * Retrieves the {@link Connection} that this pipeline is bound to.
     *
     * @return the associated {@link Connection} instance.
     */
    Connection getConnection();

    /**
     * Removes the {@link Handler} with the specified name from the pipeline.
     *
     * @param name the unique name of the handler to remove.
     * @return this {@code Pipeline} instance for method chaining.
     * @throws IllegalArgumentException if no handler with the specified name exists in the
     *     pipeline.
     */
    Pipeline remove(String name);
}
