/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

/**
 * Netty-based transport implementation for high-performance D-Bus communication.
 *
 * <p>This package provides the concrete implementation of the D-Bus client using
 * the Netty framework for efficient, asynchronous network communication. It handles
 * low-level transport details including connection management, message framing,
 * and protocol handling.
 *
 * <h2>Getting Started</h2>
 *
 * <p><strong>For first-time users:</strong> The main class you'll use is {@link NettyConnection}.
 * Use {@link NettyConnection#newSystemBusConnection()} or {@link NettyConnection#newSessionBusConnection()}
 * to create connections. Most other classes in this package are internal implementation details.</p>
 *
 * <h2>Core Components</h2>
 *
 * <h3>Connection Implementation</h3>
 * <p>The {@link NettyConnection} class provides the main entry point for D-Bus
 * connections, implementing the {@link com.lucimber.dbus.connection.Connection}
 * interface with Netty-based transport:
 *
 * <pre>{@code
 * // System bus connection
 * Connection systemBus = NettyConnection.newSystemBusConnection();
 *
 * // Session bus connection
 * Connection sessionBus = NettyConnection.newSessionBusConnection();
 *
 * // Custom connection with configuration
 * ConnectionConfig config = ConnectionConfig.builder()
 *     .withAutoReconnectEnabled(true)
 *     .withConnectTimeout(Duration.ofSeconds(10))
 *     .build();
 * Connection connection = NettyConnection.newConnection(socketAddress, config);
 * }</pre>
 *
 * <h3>Transport Configuration</h3>
 * <p>The Netty transport can be configured for optimal performance:
 *
 * <pre>{@code
 * // Configure transport-specific options
 * NettyConnectionConfig config = NettyConnectionConfig.builder()
 *     .withChannelOption(ChannelOption.TCP_NODELAY, true)
 *     .withChannelOption(ChannelOption.SO_KEEPALIVE, true)
 *     .withChannelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
 *     .withWorkerThreads(4)
 *     .withBossThreads(1)
 *     .build();
 * }</pre>
 *
 * <h2>Pipeline Architecture</h2>
 *
 * <p>The Netty implementation uses a handler pipeline for processing D-Bus messages:
 *
 * <pre>{@code
 * // Pipeline structure (inbound direction):
 * Socket → FrameDecoder → MessageDecoder → AuthHandler → UserHandlers → Application
 *
 * // Pipeline structure (outbound direction):
 * Application → UserHandlers → AuthHandler → MessageEncoder → FrameEncoder → Socket
 * }</pre>
 *
 * <h3>Built-in Handlers</h3>
 * <ul>
 * <li><strong>Frame Decoder/Encoder:</strong> Handles D-Bus message framing</li>
 * <li><strong>Message Decoder/Encoder:</strong> Converts between bytes and D-Bus messages</li>
 * <li><strong>Authentication Handler:</strong> Manages SASL authentication</li>
 * <li><strong>Error Handler:</strong> Handles protocol and network errors</li>
 * <li><strong>Health Check Handler:</strong> Monitors connection health</li>
 * </ul>
 *
 * <h2>Performance Features</h2>
 *
 * <h3>Connection Pooling</h3>
 * <pre>{@code
 * // Connection pool for shared resources
 * ConnectionPool pool = NettyConnectionPool.builder()
 *     .withMinConnections(2)
 *     .withMaxConnections(10)
 *     .withConnectionTimeout(Duration.ofSeconds(30))
 *     .build();
 *
 * Connection connection = pool.acquire();
 * try {
 *     // Use connection
 * } finally {
 *     pool.release(connection);
 * }
 * }</pre>
 *
 * <h3>Native Transport</h3>
 * <p>Automatic detection and use of native transport libraries:
 *
 * <pre>{@code
 * // Uses epoll on Linux, kqueue on macOS, NIO on other platforms
 * TransportFactory factory = TransportFactory.getBestAvailable();
 * EventLoopGroup group = factory.createEventLoopGroup(4);
 * }</pre>
 *
 * <h2>Message Processing</h2>
 *
 * <h3>Asynchronous Operations</h3>
 * <p>All operations are non-blocking and return CompletableFuture:
 *
 * <pre>{@code
 * // Asynchronous connection establishment
 * CompletableFuture<Void> connectFuture = connection.connect();
 *
 * // Asynchronous method calls
 * CompletableFuture<InboundMessage> responseFuture = connection.sendRequest(methodCall);
 *
 * // Chaining operations
 * connectFuture
 *     .thenCompose(v -> connection.sendRequest(methodCall))
 *     .thenAccept(response -> processResponse(response))
 *     .exceptionally(throwable -> {
 *         handleError(throwable);
 *         return null;
 *     });
 * }</pre>
 *
 * <h3>Backpressure Handling</h3>
 * <p>Built-in backpressure mechanisms prevent memory overflow:
 *
 * <pre>{@code
 * // Configure backpressure settings
 * NettyConnectionConfig config = NettyConnectionConfig.builder()
 *     .withWriteBufferHighWaterMark(64 * 1024)
 *     .withWriteBufferLowWaterMark(32 * 1024)
 *     .withMaxPendingWrites(1000)
 *     .build();
 * }</pre>
 *
 * <h2>Error Handling</h2>
 *
 * <p>Comprehensive error handling for network and protocol issues:
 *
 * <pre>{@code
 * // Connection event listener
 * connection.addConnectionEventListener((conn, event) -> {
 *     switch (event.getType()) {
 *         case CONNECTION_LOST:
 *             System.err.println("Connection lost: " + event.getCause());
 *             break;
 *         case RECONNECTION_FAILED:
 *             System.err.println("Reconnection failed: " + event.getCause());
 *             break;
 *         case PROTOCOL_ERROR:
 *             System.err.println("Protocol error: " + event.getCause());
 *             break;
 *     }
 * });
 * }</pre>
 *
 * <h2>Transport Types</h2>
 *
 * <h3>Unix Domain Sockets</h3>
 * <p>Primary transport for local D-Bus communication:
 *
 * <pre>{@code
 * // Connect to system bus via Unix domain socket
 * SocketAddress systemBusAddress = new DomainSocketAddress("/var/run/dbus/system_bus_socket");
 * Connection connection = NettyConnection.newConnection(systemBusAddress);
 * }</pre>
 *
 * <h3>TCP Sockets</h3>
 * <p>Network transport for remote D-Bus communication:
 *
 * <pre>{@code
 * // Connect to remote D-Bus daemon via TCP
 * SocketAddress tcpAddress = new InetSocketAddress("remote-host", 55556);
 * Connection connection = NettyConnection.newConnection(tcpAddress);
 * }</pre>
 *
 * <h2>Resource Management</h2>
 *
 * <p>Proper resource cleanup and lifecycle management:
 *
 * <pre>{@code
 * // Graceful shutdown
 * connection.close().toCompletableFuture().get(5, TimeUnit.SECONDS);
 *
 * // Shutdown with timeout
 * connection.closeGracefully(Duration.ofSeconds(5))
 *     .exceptionally(throwable -> {
 *         // Force shutdown if graceful shutdown fails
 *         connection.closeNow();
 *         return null;
 *     });
 * }</pre>
 *
 * <h2>Monitoring and Debugging</h2>
 *
 * <p>Built-in support for monitoring and debugging:
 *
 * <pre>{@code
 * // Enable wire-level logging
 * NettyConnectionConfig config = NettyConnectionConfig.builder()
 *     .withWireLoggingEnabled(true)
 *     .withWireLogLevel(LogLevel.DEBUG)
 *     .build();
 *
 * // Connection metrics
 * ConnectionMetrics metrics = connection.getMetrics();
 * System.out.println("Messages sent: " + metrics.getMessagesSent());
 * System.out.println("Messages received: " + metrics.getMessagesReceived());
 * System.out.println("Connection uptime: " + metrics.getUptime());
 * }</pre>
 *
 * <h2>Thread Model</h2>
 *
 * <p>Netty's event-driven thread model provides high concurrency:
 *
 * <ul>
 * <li><strong>Boss Thread:</strong> Accepts incoming connections</li>
 * <li><strong>Worker Threads:</strong> Handle I/O operations and message processing</li>
 * <li><strong>Application Threads:</strong> Handle user callbacks and business logic</li>
 * </ul>
 *
 * <pre>{@code
 * // Custom thread pool configuration
 * EventLoopGroup bossGroup = new NioEventLoopGroup(1);
 * EventLoopGroup workerGroup = new NioEventLoopGroup(4);
 *
 * NettyConnectionConfig config = NettyConnectionConfig.builder()
 *     .withBossGroup(bossGroup)
 *     .withWorkerGroup(workerGroup)
 *     .build();
 * }</pre>
 *
 * @see NettyConnection
 * @see com.lucimber.dbus.connection.Connection
 * @see com.lucimber.dbus.connection.ConnectionConfig
 * @since 1.0
 */
package com.lucimber.dbus.netty;
