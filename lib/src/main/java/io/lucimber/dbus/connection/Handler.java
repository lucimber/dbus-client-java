package io.lucimber.dbus.connection;

import io.lucimber.dbus.message.InboundMessage;
import io.lucimber.dbus.message.OutboundMessage;

/**
 * A handler is assigned to a pipeline and reacts to events that arise within this pipeline.
 *
 * @see Pipeline
 * @see HandlerContext
 */
public interface Handler {

    /**
     * Processes the activation of the D-Bus connection
     * or forwards it to the next receiver on the {@link Pipeline}.
     * Gets called after the connection was established and the SASL authentication was successful.
     *
     * @param ctx the {@link HandlerContext} of this {@link Handler}
     */
    default void onConnectionActive(final HandlerContext ctx) {
        ctx.passConnectionActiveEvent();
    }

    /**
     * Processes the inactivation of the D-Bus connection
     * or forwards it to the next receiver on the {@link Pipeline}.
     * Gets called after the connection was closed.
     *
     * @param ctx the {@link HandlerContext} of this {@link Handler}
     */
    default void onConnectionInactive(final HandlerContext ctx) {
        ctx.passConnectionInactiveEvent();
    }

    /**
     * Processes an inbound message or propagates the message further up the pipeline,
     * eventually reaching the last handler.
     *
     * @param ctx the {@link HandlerContext} of this {@link Handler}
     * @param msg the {@link InboundMessage}
     * @throws Exception If an inbound message could not be processed correctly.
     */
    default void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) throws Exception {
        ctx.passInboundMessage(msg);
    }

    /**
     * Processes an outbound message or propagates the message further down the pipeline,
     * eventually reaching the first handler.
     *
     * @param ctx the {@link HandlerContext} of this {@link Handler}
     * @param msg the {@link OutboundMessage}
     * @throws Exception If an outbound message could not be processed correctly.
     */
    default void onOutboundMessage(final HandlerContext ctx, final OutboundMessage msg) throws Exception {
        ctx.passOutboundMessage(msg);
    }

    /**
     * Processes the user event or forwards it to the next receiver on the {@link Pipeline}.
     *
     * @param ctx   the {@link HandlerContext} of this {@link Handler}
     * @param event the user event
     */
    default void onUserEvent(final HandlerContext ctx, final Object event) {
        ctx.passUserEvent(event);
    }

    /**
     * Processes the cause of an inbound failure or propagates the cause further up the pipeline,
     * eventually reaching the last handler.
     *
     * @param ctx   the {@link HandlerContext} of this {@link Handler}
     * @param cause the cause of the failure
     */
    default void onInboundFailure(final HandlerContext ctx, final Throwable cause) {
        ctx.passInboundFailure(cause);
    }

    /**
     * Processes the cause of an outbound failure or propagates the cause further up the pipeline,
     * eventually reaching the last handler.
     *
     * @param ctx   the {@link HandlerContext} of this {@link Handler}
     * @param cause the cause of the failure
     */
    default void onOutboundFailure(final HandlerContext ctx, final Throwable cause) {
        ctx.passOutboundFailure(cause);
    }
}
