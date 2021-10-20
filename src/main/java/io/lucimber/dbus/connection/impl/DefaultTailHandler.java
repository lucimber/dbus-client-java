package io.lucimber.dbus.connection.impl;

import io.lucimber.dbus.connection.Handler;
import io.lucimber.dbus.connection.HandlerContext;
import io.lucimber.dbus.message.InboundMessage;
import io.lucimber.dbus.message.OutboundMessage;
import io.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

final class DefaultTailHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void onConnectionActive(final HandlerContext ctx) {
        LoggerUtils.info(LOGGER, () -> "Discarding connection active event that reached the end of the pipeline.");
    }

    @Override
    public void onConnectionInactive(final HandlerContext ctx) {
        LoggerUtils.info(LOGGER, () -> "Discarding connection inactive event that reached the end of the pipeline.");
    }

    @Override
    public void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
        LoggerUtils.warn(LOGGER, () -> {
            final String format = "Discarding an inbound message that reached the end of the pipeline: %s";
            return String.format(format, msg);
        });
    }

    @Override
    public void onUserEvent(final HandlerContext ctx, final Object event) {
        LoggerUtils.warn(LOGGER, () -> {
            final String format = "Discarding an user event that reached the end of the pipeline: %s";
            return String.format(format, event);
        });
    }

    @Override
    public void onInboundFailure(final HandlerContext ctx, final Throwable cause) {
        LoggerUtils.warn(LOGGER, () -> {
            final String format = "Discarding cause of an inbound failure that reached the end of the pipeline: %s";
            return String.format(format, cause);
        });
    }

    @Override
    public void onOutboundFailure(final HandlerContext ctx, final Throwable cause) {
        LoggerUtils.warn(LOGGER, () -> {
            final String format = "Discarding cause of an outbound failure that reached the end of the pipeline: %s";
            return String.format(format, cause);
        });
    }

    @Override
    public void onOutboundMessage(final HandlerContext ctx, final OutboundMessage msg) {
        ctx.passOutboundMessage(msg);
    }
}
