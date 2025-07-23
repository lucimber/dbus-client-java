/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import org.slf4j.Logger;

import com.lucimber.dbus.message.InboundMessage;

/**
 * A skeletal implementation of the {@link InboundHandler} interface.
 *
 * <p>This abstract base class provides default (no-op) implementations of all lifecycle methods
 * except for the core inbound handling methods, which must be implemented by subclasses.
 *
 * <p>Extend this class to simplify the creation of handlers that only need to process inbound
 * messages and optionally handle connection or pipeline events.
 *
 * <p>The default implementations of all methods simply propagate events through the pipeline
 * without any additional processing, making this class suitable as a base for handlers that only
 * need to override specific methods.
 *
 * @see InboundHandler
 * @see Context
 * @see Pipeline
 * @since 1.0.0
 */
public abstract class AbstractInboundHandler implements InboundHandler {

    /**
     * Default implementation that simply propagates the failure through the pipeline.
     *
     * @param ctx the {@link Context} this handler is bound to
     * @param cause the {@link Throwable} describing the failure
     * @since 1.0.0
     */
    @Override
    public void handleInboundFailure(Context ctx, Throwable cause) {
        getLogger()
                .debug(
                        "Received a failure caused by an inbound message. "
                                + "No action was taken. "
                                + "Propagating the failure to the next component.");
        ctx.propagateInboundFailure(cause);
    }

    /**
     * Default implementation that simply propagates the message through the pipeline.
     *
     * @param ctx the {@link Context} this handler is bound to
     * @param msg the {@link InboundMessage} being processed
     * @since 1.0.0
     */
    @Override
    public void handleInboundMessage(Context ctx, InboundMessage msg) {
        getLogger()
                .debug(
                        "Inbound message received. "
                                + "No handler at this stage processed it. "
                                + "Propagating toward the pipeline head.");
        ctx.propagateInboundMessage(msg);
    }

    /**
     * Default implementation that simply propagates the user event through the pipeline.
     *
     * @param ctx the {@link Context} this handler is bound to
     * @param evt the user-defined event object
     * @since 1.0.0
     */
    @Override
    public void handleUserEvent(Context ctx, Object evt) {
        getLogger()
                .debug(
                        "User-defined event received. "
                                + "No handler at this stage processed it. "
                                + "Propagating toward the pipeline tail.");
        ctx.propagateUserEvent(evt);
    }

    /**
     * Default implementation that simply propagates the connection active event through the
     * pipeline.
     *
     * @param ctx the {@link Context} this handler is bound to
     * @since 1.0.0
     */
    @Override
    public void onConnectionActive(Context ctx) {
        getLogger()
                .debug(
                        "Connection-active event received. "
                                + "No handler at this stage processed it. "
                                + "Propagating toward the pipeline tail.");
        ctx.propagateConnectionActive();
    }

    /**
     * Default implementation that simply propagates the connection inactive event through the
     * pipeline.
     *
     * @param ctx the {@link Context} this handler is bound to
     * @since 1.0.0
     */
    @Override
    public void onConnectionInactive(Context ctx) {
        getLogger()
                .debug(
                        "Connection-inactive event received. "
                                + "No handler at this stage processed it. "
                                + "Propagating toward the pipeline tail.");
        ctx.propagateConnectionInactive();
    }

    /**
     * Default implementation that logs the handler addition event.
     *
     * @param ctx the {@link Context} this handler is bound to
     * @since 1.0.0
     */
    @Override
    public void onHandlerAdded(Context ctx) {
        getLogger().debug("I have been added to a pipeline. Context: {}", ctx.getName());
    }

    /**
     * Default implementation that logs the handler removal event.
     *
     * @param ctx the {@link Context} this handler is bound to
     * @since 1.0.0
     */
    @Override
    public void onHandlerRemoved(Context ctx) {
        getLogger().debug("I have been removed from a pipeline. Context: {}", ctx.getName());
    }

    /**
     * Returns the logger of the concrete class.
     *
     * @return the logger instance for this handler
     * @since 1.0.0
     */
    protected abstract Logger getLogger();
}
