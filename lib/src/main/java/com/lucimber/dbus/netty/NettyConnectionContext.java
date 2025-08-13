/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.ConnectionContext;
import com.lucimber.dbus.connection.ConnectionState;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.InboundMessage;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty-specific implementation of ConnectionContext that bridges the strategy pattern with the
 * existing NettyConnection infrastructure.
 */
final class NettyConnectionContext implements ConnectionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnectionContext.class);

    private final Pipeline pipeline;
    private final ExecutorService applicationTaskExecutor;
    private final NettyConnection connection;

    NettyConnectionContext(
            Pipeline pipeline,
            ExecutorService applicationTaskExecutor,
            NettyConnection connection) {
        this.pipeline = Objects.requireNonNull(pipeline);
        this.applicationTaskExecutor = Objects.requireNonNull(applicationTaskExecutor);
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Gets the underlying NettyConnection instance. This method is used by strategy implementations
     * to access the connection.
     *
     * @return the NettyConnection instance
     */
    NettyConnection getConnection() {
        return connection;
    }

    @Override
    public void onMessageReceived(InboundMessage message) {
        LOGGER.debug(
                "Message received via strategy pattern: {}", message.getClass().getSimpleName());

        // Submit to application task executor for processing
        applicationTaskExecutor.submit(
                () -> {
                    try {
                        // Route message through the pipeline
                        pipeline.propagateInboundMessage(message);
                    } catch (Exception e) {
                        LOGGER.error("Error processing inbound message", e);
                        onError(e);
                    }
                });
    }

    @Override
    public void onStateChanged(ConnectionState newState) {
        LOGGER.debug("Connection state changed to: {}", newState);

        // Notify the connection about state changes
        // This would integrate with the health handler if present
        connection.notifyStateChanged(newState);
    }

    @Override
    public void onError(Throwable error) {
        LOGGER.error("Connection error occurred", error);

        // Notify the connection about errors
        connection.notifyError(error);
    }

    @Override
    public void onConnectionEstablished() {
        LOGGER.info("Connection successfully established via strategy pattern");

        // Notify pipeline that connection is active - this will update health handler state
        pipeline.propagateConnectionActive();

        // Notify connection handlers
        connection.notifyConnectionEstablished();
    }

    @Override
    public void onConnectionLost() {
        LOGGER.warn("Connection lost");

        // Notify pipeline that connection is inactive
        pipeline.propagateConnectionInactive();

        // Trigger reconnection if enabled
        connection.notifyConnectionLost();
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }
}
