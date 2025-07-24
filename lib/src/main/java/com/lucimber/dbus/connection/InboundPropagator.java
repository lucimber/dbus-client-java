/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;

/**
 * Defines a component that can forward inbound events through a {@link Pipeline}.
 *
 * <p>Typically implemented by {@link Context} instances or internal pipeline nodes, this interface
 * allows event propagation toward the tail of the pipeline.
 *
 * @see Context
 * @see InboundMessage
 * @see Pipeline
 */
public interface InboundPropagator {

    /**
     * Propagates a connection-activated event to the next handler in the pipeline.
     *
     * <p>If not intercepted, this event will reach the tail handler.
     */
    void propagateConnectionActive();

    /**
     * Propagates a connection-inactivated event to the next handler in the pipeline.
     *
     * <p>If not intercepted, this event will reach the tail handler.
     */
    void propagateConnectionInactive();

    /**
     * Propagates an inbound message to the next handler in the pipeline.
     *
     * <p>This allows handlers to process or forward the message as needed.
     *
     * @param msg the {@link InboundMessage} to propagate.
     */
    void propagateInboundMessage(InboundMessage msg);

    /**
     * Propagates an exception or failure encountered during inbound message processing.
     *
     * <p>If not intercepted, the failure will be delivered to the final handler in the chain.
     *
     * @param cause the {@link Throwable} representing the failure.
     */
    void propagateInboundFailure(Throwable cause);
}
