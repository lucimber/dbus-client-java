package io.lucimber.dbus.connection.impl;

import io.lucimber.dbus.connection.Connection;
import io.lucimber.dbus.connection.Handler;
import io.lucimber.dbus.connection.HandlerContext;
import io.lucimber.dbus.message.OutboundMessage;
import io.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

final class DefaultHeadHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Connection connection;

    DefaultHeadHandler(final Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    @Override
    public void onOutboundMessage(final HandlerContext ctx, final OutboundMessage msg) {
        LoggerUtils.debug(LOGGER, () -> "Passing an outbound message to the connection.");
        connection.writeOutboundMessage(msg);
    }
}
