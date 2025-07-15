/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

/**
 * A high-performance, asynchronous D-Bus client library for Java applications.
 * 
 * <p>This framework enables Java applications to communicate with D-Bus services
 * in a type-safe and efficient manner. Built on top of Netty, it provides:
 * 
 * <ul>
 * <li><strong>Asynchronous Operations:</strong> All operations return {@link java.util.concurrent.CompletionStage}
 *     to avoid blocking threads</li>
 * <li><strong>Type Safety:</strong> Strong typing prevents D-Bus marshalling errors at compile time</li>
 * <li><strong>Extensibility:</strong> Handler-based pipeline allows custom message processing</li>
 * <li><strong>Performance:</strong> Native transport optimization and efficient memory usage</li>
 * <li><strong>Cross-Platform:</strong> Works reliably across different operating systems</li>
 * </ul>
 * 
 * <h2>Quick Start</h2>
 * 
 * <p>Basic connection and method call example:
 * 
 * <pre>{@code
 * // Create connection to system bus
 * Connection connection = NettyConnection.newSystemBusConnection();
 * 
 * // Connect asynchronously
 * connection.connect().toCompletableFuture().get();
 * 
 * // Create method call
 * OutboundMethodCall call = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withMember(DBusString.valueOf("GetId"))
 *     .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withReplyExpected(true)
 *     .build();
 * 
 * // Send request and handle response
 * CompletableFuture<InboundMessage> response = connection.sendRequest(call);
 * response.thenAccept(reply -> {
 *     if (reply instanceof InboundMethodReturn) {
 *         System.out.println("Success: " + reply);
 *     } else if (reply instanceof InboundError) {
 *         System.err.println("Error: " + ((InboundError) reply).getErrorName());
 *     }
 * });
 * 
 * // Clean up
 * connection.close();
 * }</pre>
 * 
 * <h2>Core Packages</h2>
 * 
 * <dl>
 * <dt>{@link com.lucimber.dbus.connection}</dt>
 * <dd>Connection management, pipeline handlers, and configuration</dd>
 * 
 * <dt>{@link com.lucimber.dbus.message}</dt>
 * <dd>D-Bus message types (method calls, returns, signals, errors)</dd>
 * 
 * <dt>{@link com.lucimber.dbus.type}</dt>
 * <dd>Type-safe D-Bus data types and containers</dd>
 * 
 * <dt>{@link com.lucimber.dbus.encoder}</dt>
 * <dd>D-Bus message serialization</dd>
 * 
 * <dt>{@link com.lucimber.dbus.decoder}</dt>
 * <dd>D-Bus message deserialization</dd>
 * 
 * <dt>{@link com.lucimber.dbus.netty}</dt>
 * <dd>Netty-based transport implementation</dd>
 * </dl>
 * 
 * <h2>Authentication</h2>
 * 
 * <p>The framework supports multiple SASL authentication mechanisms:
 * <ul>
 * <li><strong>EXTERNAL:</strong> Unix credential-based authentication (default on Linux)</li>
 * <li><strong>DBUS_COOKIE_SHA1:</strong> Cookie-based authentication</li>
 * <li><strong>ANONYMOUS:</strong> Anonymous access (limited scenarios)</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * 
 * <p>The framework provides comprehensive error handling for:
 * <ul>
 * <li>Network and connection errors</li>
 * <li>D-Bus protocol violations</li>
 * <li>Authentication failures</li>
 * <li>Application-level D-Bus errors</li>
 * <li>Timeout and resource exhaustion</li>
 * </ul>
 * 
 * @see com.lucimber.dbus.connection.Connection
 * @see com.lucimber.dbus.netty.NettyConnection
 * @see com.lucimber.dbus.message.OutboundMethodCall
 * @see com.lucimber.dbus.type
 * @since 1.0
 */
package com.lucimber.dbus;
