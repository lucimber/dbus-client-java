/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection;

/**
 * A handler that can be attached to a {@link Pipeline} to intercept and respond to connection state
 * changes and user-defined events.
 *
 * <p>Implementations of this interface are notified about lifecycle events and can perform
 * processing based on their role in the pipeline.
 */
public interface Handler {

    /**
     * Invoked when the connection associated with this context becomes active.
     *
     * @param ctx the {@link Context} this handler is bound to.
     */
    void onConnectionActive(Context ctx);

    /**
     * Invoked when the connection associated with this context becomes inactive.
     *
     * @param ctx the {@link Context} this handler is bound to.
     */
    void onConnectionInactive(Context ctx);

    /**
     * Invoked when this handler is added to the pipeline.
     *
     * <p>This is typically the first lifecycle method called for a handler.
     *
     * @param ctx the {@link Context} this handler is bound to.
     */
    void onHandlerAdded(Context ctx);

    /**
     * Invoked just before this handler is removed from the pipeline.
     *
     * <p>This allows the handler to perform any necessary cleanup.
     *
     * @param ctx the {@link Context} this handler is bound to.
     */
    void onHandlerRemoved(Context ctx);

    /**
     * Invoked when a user-defined event is propagated through the pipeline.
     *
     * <p>Handlers can choose to react to or forward the event, depending on their logic.
     *
     * @param ctx the {@link Context} this handler is bound to.
     * @param evt the user-defined event to handle.
     */
    void handleUserEvent(Context ctx, Object evt);
}
