/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.util.LoggerUtils;

/**
 * Handler responsible for managing pipeline handlers during reconnection. This handler listens for
 * reconnection events and re-adds handlers that were removed during the connection process, such as
 * SASL handlers.
 */
public final class ReconnectionHandlerManager extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReconnectionHandlerManager.class);
    private final Promise<Void> connectPromise;

    public ReconnectionHandlerManager(Promise<Void> connectPromise) {
        this.connectPromise = connectPromise;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == DBusChannelEvent.RECONNECTION_STARTING) {
            handleReconnectionStarting(ctx);
        } else if (evt == DBusChannelEvent.RECONNECTION_HANDLERS_READD_REQUIRED) {
            handleReconnectionHandlersReaddRequired(ctx);
        }

        // Always propagate the event
        super.userEventTriggered(ctx, evt);
    }

    private void handleReconnectionStarting(ChannelHandlerContext ctx) {
        LOGGER.debug(LoggerUtils.CONNECTION, "Reconnection starting - preparing pipeline");

        // Reset channel attributes for reconnection
        ctx.channel().attr(DBusChannelAttribute.SERIAL_COUNTER).set(new AtomicLong(1));
        ctx.channel().attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).set(null);

        LOGGER.debug(LoggerUtils.CONNECTION, "Channel attributes reset for reconnection");
    }

    private void handleReconnectionHandlersReaddRequired(ChannelHandlerContext ctx) {
        LOGGER.debug(
                LoggerUtils.CONNECTION, "Re-adding handlers that were removed during connection");

        // Re-add SASL handlers in the correct order if they don't exist
        addSaslHandlersIfMissing(ctx);

        // Update ConnectionCompletionHandler with new promise
        updateConnectionCompletionHandler(ctx);

        LOGGER.info(LoggerUtils.CONNECTION, "Handlers re-added for reconnection");
    }

    private void addSaslHandlersIfMissing(ChannelHandlerContext ctx) {
        // Use centralized configuration to ensure consistent ordering
        Map<String, Supplier<ChannelHandler>> saslHandlers =
                DBusHandlerConfiguration.getSaslHandlers();

        // Find the first D-Bus handler to use as insertion point
        String firstDbusHandler = DBusHandlerConfiguration.findFirstDbusHandler(ctx.pipeline());

        String previousHandler = null;
        for (Map.Entry<String, Supplier<ChannelHandler>> entry : saslHandlers.entrySet()) {
            String handlerName = entry.getKey();

            // Check if handler is missing from pipeline
            if (ctx.pipeline().get(handlerName) == null) {
                ChannelHandler handler = entry.getValue().get();

                if (previousHandler != null) {
                    // Add after the previous SASL handler
                    ctx.pipeline().addAfter(previousHandler, handlerName, handler);
                } else if (firstDbusHandler != null) {
                    // Add before the first D-Bus handler
                    ctx.pipeline().addBefore(firstDbusHandler, handlerName, handler);
                } else {
                    // Fallback: add at the beginning
                    ctx.pipeline().addFirst(handlerName, handler);
                }

                LOGGER.debug(LoggerUtils.HANDLER_LIFECYCLE, "Re-added {}", handlerName);
            }

            previousHandler = handlerName;
        }
    }

    private void updateConnectionCompletionHandler(ChannelHandlerContext ctx) {
        ConnectionCompletionHandler completionHandler =
                (ConnectionCompletionHandler)
                        ctx.pipeline().get(DBusHandlerNames.CONNECTION_COMPLETION_HANDLER);
        if (completionHandler != null) {
            completionHandler.reset(connectPromise);
            LOGGER.debug(
                    LoggerUtils.HANDLER_LIFECYCLE,
                    "Updated ConnectionCompletionHandler with new promise");
        }
    }
}
