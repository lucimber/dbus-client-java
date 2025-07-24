/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty;

import com.lucimber.dbus.netty.sasl.SaslAuthenticationHandler;
import com.lucimber.dbus.netty.sasl.SaslCodec;
import com.lucimber.dbus.netty.sasl.SaslInitiationHandler;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized configuration for D-Bus pipeline handlers. This class maintains the definitive order
 * and creation logic for all handlers, ensuring perfect synchronization between initial setup and
 * reconnection scenarios.
 */
public final class DBusHandlerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBusHandlerConfiguration.class);

    /**
     * Defines the complete handler pipeline order and creation logic. SASL handlers are added
     * first, followed by D-Bus protocol handlers.
     */
    private static Map<String, Supplier<ChannelHandler>> createHandlerMap(
            Promise<Void> connectPromise, RealityCheckpoint appLogicHandler) {

        Map<String, Supplier<ChannelHandler>> handlers = new LinkedHashMap<>();

        // SASL handlers (added first, removed after SASL completion)
        handlers.put(DBusHandlerNames.SASL_INITIATION_HANDLER, SaslInitiationHandler::new);
        handlers.put(DBusHandlerNames.SASL_CODEC, SaslCodec::new);
        handlers.put(DBusHandlerNames.SASL_AUTHENTICATION_HANDLER, SaslAuthenticationHandler::new);

        // D-Bus protocol handlers (permanent)
        handlers.put(
                DBusHandlerNames.NETTY_BYTE_LOGGER,
                () -> new LoggingHandler(LoggerUtils.TRANSPORT.getName(), LogLevel.DEBUG));
        handlers.put(DBusHandlerNames.FRAME_ENCODER, FrameEncoder::new);
        handlers.put(DBusHandlerNames.OUTBOUND_MESSAGE_ENCODER, OutboundMessageEncoder::new);
        handlers.put(DBusHandlerNames.FRAME_DECODER, FrameDecoder::new);
        handlers.put(DBusHandlerNames.INBOUND_MESSAGE_DECODER, InboundMessageDecoder::new);
        handlers.put(DBusHandlerNames.DBUS_MANDATORY_NAME_HANDLER, DBusMandatoryNameHandler::new);
        handlers.put(
                DBusHandlerNames.CONNECTION_COMPLETION_HANDLER,
                () -> new ConnectionCompletionHandler(connectPromise));

        // Reconnection management handler
        handlers.put(
                "ReconnectionHandlerManager", () -> new ReconnectionHandlerManager(connectPromise));

        // Application logic handler (always last)
        if (appLogicHandler != null) {
            handlers.put("RealityCheckpoint", () -> appLogicHandler);
        }

        return handlers;
    }

    /**
     * Initializes the complete pipeline in the correct order.
     *
     * @param pipeline the channel pipeline to configure
     * @param connectPromise the promise to complete when connection is established
     * @param appLogicHandler the application logic handler
     */
    public static void initializePipeline(
            ChannelPipeline pipeline,
            Promise<Void> connectPromise,
            RealityCheckpoint appLogicHandler) {
        LOGGER.debug(
                LoggerUtils.HANDLER_LIFECYCLE,
                "Initializing D-Bus pipeline with centralized configuration");

        Map<String, Supplier<ChannelHandler>> handlers =
                createHandlerMap(connectPromise, appLogicHandler);

        for (Map.Entry<String, Supplier<ChannelHandler>> entry : handlers.entrySet()) {
            String name = entry.getKey();
            ChannelHandler handler = entry.getValue().get();
            pipeline.addLast(name, handler);
            LOGGER.debug(LoggerUtils.HANDLER_LIFECYCLE, "Added handler: {}", name);
        }
    }

    /**
     * Gets the ordered list of SASL handler names that need to be re-added during reconnection.
     *
     * @return ordered map of SASL handler names to their creation suppliers
     */
    public static Map<String, Supplier<ChannelHandler>> getSaslHandlers() {
        Map<String, Supplier<ChannelHandler>> saslHandlers = new LinkedHashMap<>();
        saslHandlers.put(DBusHandlerNames.SASL_INITIATION_HANDLER, SaslInitiationHandler::new);
        saslHandlers.put(DBusHandlerNames.SASL_CODEC, SaslCodec::new);
        saslHandlers.put(
                DBusHandlerNames.SASL_AUTHENTICATION_HANDLER, SaslAuthenticationHandler::new);
        return saslHandlers;
    }

    /**
     * Gets the ordered list of all handler names in the pipeline.
     *
     * @return ordered list of handler names
     */
    public static String[] getHandlerOrder() {
        return new String[] {
            DBusHandlerNames.SASL_INITIATION_HANDLER,
            DBusHandlerNames.SASL_CODEC,
            DBusHandlerNames.SASL_AUTHENTICATION_HANDLER,
            DBusHandlerNames.NETTY_BYTE_LOGGER,
            DBusHandlerNames.FRAME_ENCODER,
            DBusHandlerNames.OUTBOUND_MESSAGE_ENCODER,
            DBusHandlerNames.FRAME_DECODER,
            DBusHandlerNames.INBOUND_MESSAGE_DECODER,
            DBusHandlerNames.DBUS_MANDATORY_NAME_HANDLER,
            DBusHandlerNames.CONNECTION_COMPLETION_HANDLER,
            "ReconnectionHandlerManager",
            "RealityCheckpoint"
        };
    }

    /**
     * Finds the first existing D-Bus handler in the pipeline to use as an insertion point.
     *
     * @param pipeline the channel pipeline to search
     * @return the name of the first D-Bus handler found, or null if none found
     */
    public static String findFirstDbusHandler(ChannelPipeline pipeline) {
        String[] dbusHandlers = {
            DBusHandlerNames.NETTY_BYTE_LOGGER,
            DBusHandlerNames.FRAME_ENCODER,
            DBusHandlerNames.OUTBOUND_MESSAGE_ENCODER,
            DBusHandlerNames.FRAME_DECODER,
            DBusHandlerNames.INBOUND_MESSAGE_DECODER,
            DBusHandlerNames.DBUS_MANDATORY_NAME_HANDLER,
            DBusHandlerNames.CONNECTION_COMPLETION_HANDLER
        };

        for (String handlerName : dbusHandlers) {
            if (pipeline.get(handlerName) != null) {
                return handlerName;
            }
        }

        return null;
    }

    private DBusHandlerConfiguration() {
        // Utility class - no instances
    }
}
