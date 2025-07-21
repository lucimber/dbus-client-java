/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

/**
 * Standard D-Bus interfaces and implementations for common D-Bus functionality.
 * 
 * <p>This package provides Java implementations of standard D-Bus interfaces
 * that are commonly used across different D-Bus applications. These interfaces
 * follow the official D-Bus specification and provide type-safe access to
 * standard D-Bus functionality.
 * 
 * <h2>Getting Started</h2>
 * 
 * <p><strong>For first-time users:</strong> The {@link Properties} interface is commonly used
 * for accessing D-Bus object properties. This package will expand as more standard interfaces
 * are implemented. Most D-Bus interactions use custom interfaces specific to each service.</p>
 * 
 * <h2>Standard Interfaces</h2>
 * 
 * <h3>Introspection Interface</h3>
 * <p>The {@code org.freedesktop.DBus.Introspectable} interface allows clients
 * to discover the structure and capabilities of D-Bus objects:
 * 
 * <pre>{@code
 * // Introspect a D-Bus object
 * OutboundMethodCall introspectCall = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/example/Object"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus.Introspectable"))
 *     .withMember(DBusString.valueOf("Introspect"))
 *     .withDestination(DBusString.valueOf("org.example.Service"))
 *     .withReplyExpected(true)
 *     .build();
 * 
 * CompletableFuture<InboundMessage> response = connection.sendRequest(introspectCall);
 * response.thenAccept(reply -> {
 *     if (reply instanceof InboundMethodReturn) {
 *         InboundMethodReturn returnMsg = (InboundMethodReturn) reply;
 *         List<DBusType> body = returnMsg.getBody();
 *         if (!body.isEmpty()) {
 *             DBusString xmlData = (DBusString) body.get(0);
 *             System.out.println("Introspection XML: " + xmlData.toString());
 *         }
 *     }
 * });
 * }</pre>
 * 
 * <h3>Properties Interface</h3>
 * <p>The {@code org.freedesktop.DBus.Properties} interface provides standardized
 * property access for D-Bus objects:
 * 
 * <pre>{@code
 * // Get a property value
 * OutboundMethodCall getProperty = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/example/Object"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
 *     .withMember(DBusString.valueOf("Get"))
 *     .withDestination(DBusString.valueOf("org.example.Service"))
 *     .withBody(Arrays.asList(
 *         DBusString.valueOf("org.example.Interface"),
 *         DBusString.valueOf("PropertyName")
 *     ))
 *     .withReplyExpected(true)
 *     .build();
 * 
 * // Set a property value
 * OutboundMethodCall setProperty = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/example/Object"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
 *     .withMember(DBusString.valueOf("Set"))
 *     .withDestination(DBusString.valueOf("org.example.Service"))
 *     .withBody(Arrays.asList(
 *         DBusString.valueOf("org.example.Interface"),
 *         DBusString.valueOf("PropertyName"),
 *         DBusVariant.valueOf(DBusString.valueOf("NewValue"))
 *     ))
 *     .withReplyExpected(true)
 *     .build();
 * 
 * // Get all properties
 * OutboundMethodCall getAllProperties = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/example/Object"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
 *     .withMember(DBusString.valueOf("GetAll"))
 *     .withDestination(DBusString.valueOf("org.example.Service"))
 *     .withBody(Arrays.asList(
 *         DBusString.valueOf("org.example.Interface")
 *     ))
 *     .withReplyExpected(true)
 *     .build();
 * }</pre>
 * 
 * <h3>Peer Interface</h3>
 * <p>The {@code org.freedesktop.DBus.Peer} interface provides basic peer-to-peer
 * functionality for D-Bus connections:
 * 
 * <pre>{@code
 * // Ping a D-Bus peer
 * OutboundMethodCall ping = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
 *     .withMember(DBusString.valueOf("Ping"))
 *     .withDestination(DBusString.valueOf("org.example.Service"))
 *     .withReplyExpected(true)
 *     .build();
 * 
 * // Get machine UUID
 * OutboundMethodCall getMachineId = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
 *     .withMember(DBusString.valueOf("GetMachineId"))
 *     .withDestination(DBusString.valueOf("org.example.Service"))
 *     .withReplyExpected(true)
 *     .build();
 * }</pre>
 * 
 * <h2>Bus Interface</h2>
 * 
 * <p>The {@code org.freedesktop.DBus} interface provides access to the D-Bus
 * daemon functionality:
 * 
 * <pre>{@code
 * // List available services
 * OutboundMethodCall listNames = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withMember(DBusString.valueOf("ListNames"))
 *     .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withReplyExpected(true)
 *     .build();
 * 
 * // Request a service name
 * OutboundMethodCall requestName = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withMember(DBusString.valueOf("RequestName"))
 *     .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withBody(Arrays.asList(
 *         DBusString.valueOf("org.example.MyService"),
 *         DBusUInt32.valueOf(0) // No flags
 *     ))
 *     .withReplyExpected(true)
 *     .build();
 * 
 * // Get service owner
 * OutboundMethodCall getNameOwner = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withMember(DBusString.valueOf("GetNameOwner"))
 *     .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
 *     .withBody(Arrays.asList(
 *         DBusString.valueOf("org.example.Service")
 *     ))
 *     .withReplyExpected(true)
 *     .build();
 * }</pre>
 * 
 * <h2>Signal Handling</h2>
 * 
 * <p>Standard interfaces also define common signals:
 * 
 * <pre>{@code
 * // Handle PropertiesChanged signals
 * connection.getPipeline().addLast("properties-handler", new AbstractInboundHandler() {
 *     @Override
 *     public void handleInboundMessage(Context ctx, InboundMessage msg) {
 *         if (msg instanceof InboundSignal) {
 *             InboundSignal signal = (InboundSignal) msg;
 *             String interfaceName = signal.getInterfaceName()
 *                 .map(DBusString::toString)
 *                 .orElse("");
 *             String memberName = signal.getMember().toString();
 *             
 *             if ("org.freedesktop.DBus.Properties".equals(interfaceName) &&
 *                 "PropertiesChanged".equals(memberName)) {
 *                 handlePropertiesChanged(signal);
 *             }
 *         }
 *         ctx.propagateInboundMessage(msg);
 *     }
 *     
 *     private void handlePropertiesChanged(InboundSignal signal) {
 *         List<DBusType> arguments = signal.getBody();
 *         if (arguments.size() >= 2) {
 *             DBusString interfaceName = (DBusString) arguments.get(0);
 *             DBusDict<DBusString, DBusVariant> changedProperties = 
 *                 (DBusDict<DBusString, DBusVariant>) arguments.get(1);
 *             
 *             System.out.println("Properties changed on " + interfaceName);
 *             changedProperties.forEach((key, value) -> {
 *                 System.out.println("  " + key + " = " + value);
 *             });
 *         }
 *     }
 * });
 * 
 * // Handle NameOwnerChanged signals
 * connection.getPipeline().addLast("name-owner-handler", new AbstractInboundHandler() {
 *     @Override
 *     public void handleInboundMessage(Context ctx, InboundMessage msg) {
 *         if (msg instanceof InboundSignal) {
 *             InboundSignal signal = (InboundSignal) msg;
 *             String interfaceName = signal.getInterfaceName()
 *                 .map(DBusString::toString)
 *                 .orElse("");
 *             String memberName = signal.getMember().toString();
 *             
 *             if ("org.freedesktop.DBus".equals(interfaceName) &&
 *                 "NameOwnerChanged".equals(memberName)) {
 *                 handleNameOwnerChanged(signal);
 *             }
 *         }
 *         ctx.propagateInboundMessage(msg);
 *     }
 *     
 *     private void handleNameOwnerChanged(InboundSignal signal) {
 *         List<DBusType> arguments = signal.getBody();
 *         if (arguments.size() >= 3) {
 *             String serviceName = ((DBusString) arguments.get(0)).toString();
 *             String oldOwner = ((DBusString) arguments.get(1)).toString();
 *             String newOwner = ((DBusString) arguments.get(2)).toString();
 *             
 *             if (oldOwner.isEmpty() && !newOwner.isEmpty()) {
 *                 System.out.println("Service appeared: " + serviceName);
 *             } else if (!oldOwner.isEmpty() && newOwner.isEmpty()) {
 *                 System.out.println("Service disappeared: " + serviceName);
 *             }
 *         }
 *     }
 * });
 * }</pre>
 * 
 * <h2>Error Handling</h2>
 * 
 * <p>Standard D-Bus error names are defined for common error conditions:
 * 
 * <pre>{@code
 * // Handle standard D-Bus errors
 * private void handleStandardErrors(InboundError error) {
 *     String errorName = error.getErrorName().toString();
 *     
 *     switch (errorName) {
 *         case "org.freedesktop.DBus.Error.ServiceUnknown":
 *             System.err.println("Service not found");
 *             break;
 *         case "org.freedesktop.DBus.Error.UnknownMethod":
 *             System.err.println("Method not found");
 *             break;
 *         case "org.freedesktop.DBus.Error.UnknownInterface":
 *             System.err.println("Interface not found");
 *             break;
 *         case "org.freedesktop.DBus.Error.UnknownObject":
 *             System.err.println("Object not found");
 *             break;
 *         case "org.freedesktop.DBus.Error.InvalidArgs":
 *             System.err.println("Invalid arguments");
 *             break;
 *         case "org.freedesktop.DBus.Error.AccessDenied":
 *             System.err.println("Access denied");
 *             break;
 *         case "org.freedesktop.DBus.Error.NoReply":
 *             System.err.println("No reply received");
 *             break;
 *         default:
 *             System.err.println("D-Bus error: " + errorName);
 *     }
 * }
 * }</pre>
 * 
 * <h2>Utility Classes</h2>
 * 
 * <p>Helper classes for working with standard interfaces:
 * 
 * <pre>{@code
 * // Standard interface constants
 * public static final String INTROSPECTABLE_INTERFACE = "org.freedesktop.DBus.Introspectable";
 * public static final String PROPERTIES_INTERFACE = "org.freedesktop.DBus.Properties";
 * public static final String PEER_INTERFACE = "org.freedesktop.DBus.Peer";
 * public static final String DBUS_INTERFACE = "org.freedesktop.DBus";
 * 
 * // Helper methods for common operations
 * public static CompletableFuture<String> introspectObject(Connection connection, 
 *                                                           String serviceName, 
 *                                                           String objectPath) {
 *     // Implementation for introspection
 * }
 * 
 * public static CompletableFuture<DBusVariant> getProperty(Connection connection,
 *                                                          String serviceName,
 *                                                          String objectPath,
 *                                                          String interfaceName,
 *                                                          String propertyName) {
 *     // Implementation for property access
 * }
 * }</pre>
 * 
 * <h2>Best Practices</h2>
 * 
 * <ul>
 * <li><strong>Use Standard Interfaces:</strong> Always use these standard interfaces when available</li>
 * <li><strong>Error Handling:</strong> Handle standard D-Bus errors appropriately</li>
 * <li><strong>Property Changes:</strong> Subscribe to PropertiesChanged signals for reactive updates</li>
 * <li><strong>Service Discovery:</strong> Use introspection to discover available interfaces and methods</li>
 * <li><strong>Peer Communication:</strong> Use Peer interface for basic connectivity testing</li>
 * </ul>
 * 
 * <h2>Compatibility</h2>
 * 
 * <p>All standard interfaces in this package are compatible with:
 * 
 * <ul>
 * <li>D-Bus specification version 1.0 and later</li>
 * <li>Common D-Bus implementations (dbus-daemon, systemd, etc.)</li>
 * <li>Cross-platform D-Bus services</li>
 * <li>Both system and session bus environments</li>
 * </ul>
 * 
 * @see com.lucimber.dbus.message
 * @see com.lucimber.dbus.connection
 * @see com.lucimber.dbus.type
 * @since 1.0
 */
package com.lucimber.dbus.standard;
