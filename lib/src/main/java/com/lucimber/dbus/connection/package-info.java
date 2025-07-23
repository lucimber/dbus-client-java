/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Connection management, lifecycle, and handler pipeline for D-Bus communication.
 *
 * <p>This package provides the core abstractions for establishing and managing D-Bus connections.
 * It implements a pipeline-based architecture that allows for extensible message processing through
 * custom handlers.
 *
 * <h2>Getting Started</h2>
 *
 * <p><strong>For first-time users:</strong> Start with {@link Connection} interface documentation,
 * then see {@link com.lucimber.dbus.netty.NettyConnection} for the concrete implementation. The
 * {@link ConnectionConfig} class shows all available configuration options.
 *
 * <h2>Core Concepts</h2>
 *
 * <h3>Connection</h3>
 *
 * <p>The {@link Connection} interface is the main entry point for D-Bus communication. It provides
 * methods for:
 *
 * <ul>
 *   <li>Establishing connections to D-Bus daemons
 *   <li>Sending method calls and receiving responses
 *   <li>Managing connection lifecycle and health
 *   <li>Accessing the handler pipeline
 * </ul>
 *
 * <h3>Pipeline Architecture</h3>
 *
 * <p>The handler pipeline processes messages in a configurable chain:
 *
 * <pre>{@code
 * // Add custom handlers to the pipeline
 * Pipeline pipeline = connection.getPipeline();
 * pipeline.addLast("logger", new LoggingHandler());
 * pipeline.addLast("auth", new AuthenticationHandler());
 * pipeline.addLast("app", new ApplicationHandler());
 * }</pre>
 *
 * <h3>Handler Types</h3>
 *
 * <dl>
 *   <dt>{@link InboundHandler}
 *   <dd>Processes messages received from the D-Bus daemon
 *   <dt>{@link OutboundHandler}
 *   <dd>Processes messages being sent to the D-Bus daemon
 *   <dt>Duplex Handlers (extend {@link AbstractDuplexHandler})
 *   <dd>Handle both inbound and outbound messages
 * </dl>
 *
 * <h2>Configuration</h2>
 *
 * <p>Connection behavior can be customized through {@link ConnectionConfig}:
 *
 * <pre>{@code
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withAutoReconnectEnabled(true)
 *     .withReconnectInitialDelay(Duration.ofSeconds(1))
 *     .withMaxReconnectAttempts(10)
 *     .withHealthCheckEnabled(true)
 *     .withHealthCheckInterval(Duration.ofSeconds(30))
 *     .withConnectTimeout(Duration.ofSeconds(10))
 *     .withMethodCallTimeout(Duration.ofSeconds(30))
 *     .build();
 * }</pre>
 *
 * <h2>Connection Lifecycle</h2>
 *
 * <p>Connections follow a well-defined lifecycle:
 *
 * <ol>
 *   <li><strong>Creation:</strong> Connection instance is created with configuration
 *   <li><strong>Connection:</strong> {@link Connection#connect()} establishes the D-Bus connection
 *   <li><strong>Authentication:</strong> SASL authentication is performed automatically
 *   <li><strong>Ready:</strong> Connection is ready for message exchange
 *   <li><strong>Health Monitoring:</strong> Optional health checks ensure connection validity
 *   <li><strong>Reconnection:</strong> Automatic reconnection on connection loss (if enabled)
 *   <li><strong>Shutdown:</strong> {@link Connection#close()} cleanly closes the connection
 * </ol>
 *
 * <h2>Event Handling</h2>
 *
 * <p>Connection events can be monitored through {@link ConnectionEventListener}:
 *
 * <pre>{@code
 * connection.addConnectionEventListener((conn, event) -> {
 *     switch (event.getType()) {
 *         case CONNECTED:
 *             System.out.println("Connected to D-Bus");
 *             break;
 *         case DISCONNECTED:
 *             System.out.println("Disconnected from D-Bus");
 *             break;
 *         case RECONNECTION_ATTEMPT:
 *             System.out.println("Attempting reconnection...");
 *             break;
 *     }
 * });
 * }</pre>
 *
 * <h2>Handler Examples</h2>
 *
 * <h3>Simple Inbound Handler</h3>
 *
 * <pre>{@code
 * public class LoggingHandler extends AbstractInboundHandler {
 *     @Override
 *     public void handleInboundMessage(Context ctx, InboundMessage msg) {
 *         System.out.println("Received: " + msg.getType());
 *         ctx.propagateInboundMessage(msg); // Always propagate
 *     }
 * }
 * }</pre>
 *
 * <h3>Signal Filter Handler</h3>
 *
 * <pre>{@code
 * public class SignalFilterHandler extends AbstractInboundHandler {
 *     private final Set<String> interestedInterfaces;
 *
 *     @Override
 *     public void handleInboundMessage(Context ctx, InboundMessage msg) {
 *         if (msg instanceof InboundSignal) {
 *             InboundSignal signal = (InboundSignal) msg;
 *             String interfaceName = signal.getInterfaceName()
 *                 .map(DBusString::toString)
 *                 .orElse("");
 *
 *             if (interestedInterfaces.contains(interfaceName)) {
 *                 ctx.propagateInboundMessage(msg);
 *             }
 *             // Filtered signals are not propagated
 *         } else {
 *             ctx.propagateInboundMessage(msg);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>SASL Authentication</h2>
 *
 * <p>The {@link com.lucimber.dbus.connection.sasl} sub-package provides SASL authentication
 * mechanisms for secure D-Bus communication.
 *
 * @see Connection
 * @see ConnectionConfig
 * @see Pipeline
 * @see InboundHandler
 * @see OutboundHandler
 * @see AbstractInboundHandler
 * @see AbstractOutboundHandler
 * @see AbstractDuplexHandler
 * @since 1.0
 */
package com.lucimber.dbus.connection;
