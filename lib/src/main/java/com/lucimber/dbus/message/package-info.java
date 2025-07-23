/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * D-Bus message types and structures for method calls, returns, signals, and errors.
 *
 * <p>This package provides the core message abstractions for D-Bus communication. All D-Bus
 * interactions are based on four fundamental message types, each with specific purposes and
 * structures.
 *
 * <h2>Getting Started</h2>
 *
 * <p><strong>For first-time users:</strong> Start with {@link OutboundMethodCall.Builder} to create
 * method calls, and learn to handle {@link InboundMethodReturn} and {@link InboundError} responses.
 * For event-driven programming, explore {@link OutboundSignal} and {@link InboundSignal}.
 *
 * <h2>Message Types</h2>
 *
 * <h3>Method Calls ({@link OutboundMethodCall})</h3>
 *
 * <p>Request messages sent to invoke methods on remote D-Bus objects:
 *
 * <ul>
 *   <li>Contains target object path, interface, and method name
 *   <li>Carries typed arguments for the method invocation
 *   <li>Can optionally expect a reply message
 *   <li>Includes caller authentication information
 * </ul>
 *
 * <h3>Method Returns ({@link InboundMethodReturn})</h3>
 *
 * <p>Success response messages returned from method calls:
 *
 * <ul>
 *   <li>Contains the return values from the method execution
 *   <li>Matches the original method call via serial number
 *   <li>Provides type-safe access to return data
 *   <li>Indicates successful method completion
 * </ul>
 *
 * <h3>Error Messages ({@link InboundError})</h3>
 *
 * <p>Failure response messages indicating method call errors:
 *
 * <ul>
 *   <li>Contains error name and optional error message
 *   <li>Matches the failed method call via serial number
 *   <li>Provides structured error information
 *   <li>Follows D-Bus error naming conventions
 * </ul>
 *
 * <h3>Signal Messages ({@link InboundSignal})</h3>
 *
 * <p>Broadcast messages for event notification:
 *
 * <ul>
 *   <li>Emitted by services to notify interested clients
 *   <li>Contains event data and context information
 *   <li>Can be filtered by interface, member, or sender
 *   <li>Supports event-driven programming patterns
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Creating Method Calls</h3>
 *
 * <pre>{@code
 * // Create a method call to get system information
 * OutboundMethodCall call = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/freedesktop/hostname1"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.hostname1"))
 *     .withMember(DBusString.valueOf("GetHostname"))
 *     .withDestination(DBusString.valueOf("org.freedesktop.hostname1"))
 *     .withReplyExpected(true)
 *     .build();
 *
 * // Send the call and handle the response
 * CompletableFuture<InboundMessage> response = connection.sendRequest(call);
 * response.thenAccept(reply -> {
 *     if (reply instanceof InboundMethodReturn) {
 *         InboundMethodReturn returnMsg = (InboundMethodReturn) reply;
 *         List<DBusType> returnValues = returnMsg.getBody();
 *         if (!returnValues.isEmpty()) {
 *             DBusString hostname = (DBusString) returnValues.get(0);
 *             System.out.println("Hostname: " + hostname.toString());
 *         }
 *     } else if (reply instanceof InboundError) {
 *         InboundError error = (InboundError) reply;
 *         System.err.println("Error: " + error.getErrorName());
 *     }
 * });
 * }</pre>
 *
 * <h3>Method Call with Arguments</h3>
 *
 * <pre>{@code
 * // Create a method call with typed arguments
 * OutboundMethodCall setProperty = OutboundMethodCall.Builder
 *     .create()
 *     .withPath(DBusObjectPath.valueOf("/org/example/Object"))
 *     .withInterface(DBusString.valueOf("org.freedesktop.DBus.Properties"))
 *     .withMember(DBusString.valueOf("Set"))
 *     .withDestination(DBusString.valueOf("org.example.Service"))
 *     .withBody(Arrays.asList(
 *         DBusString.valueOf("org.example.Interface"),
 *         DBusString.valueOf("PropertyName"),
 *         DBusVariant.valueOf(DBusString.valueOf("PropertyValue"))
 *     ))
 *     .withReplyExpected(true)
 *     .build();
 * }</pre>
 *
 * <h3>Handling Signals</h3>
 *
 * <pre>{@code
 * // Install a signal handler in the pipeline
 * connection.getPipeline().addLast("signal-handler", new AbstractInboundHandler() {
 *     @Override
 *     public void handleInboundMessage(Context ctx, InboundMessage msg) {
 *         if (msg instanceof InboundSignal) {
 *             InboundSignal signal = (InboundSignal) msg;
 *
 *             // Check signal details
 *             String interfaceName = signal.getInterfaceName()
 *                 .map(DBusString::toString)
 *                 .orElse("unknown");
 *             String memberName = signal.getMember().toString();
 *
 *             System.out.println("Signal: " + interfaceName + "." + memberName);
 *
 *             // Process signal arguments
 *             List<DBusType> arguments = signal.getBody();
 *             for (int i = 0; i < arguments.size(); i++) {
 *                 System.out.println("  arg" + i + ": " + arguments.get(i));
 *             }
 *         }
 *
 *         ctx.propagateInboundMessage(msg);
 *     }
 * });
 * }</pre>
 *
 * <h3>Error Handling</h3>
 *
 * <pre>{@code
 * // Handle different types of errors
 * private void handleMethodResponse(InboundMessage response) {
 *     if (response instanceof InboundMethodReturn) {
 *         InboundMethodReturn returnMsg = (InboundMethodReturn) response;
 *         processSuccessfulResponse(returnMsg);
 *
 *     } else if (response instanceof InboundError) {
 *         InboundError error = (InboundError) response;
 *         String errorName = error.getErrorName().toString();
 *
 *         // Handle specific error types
 *         switch (errorName) {
 *             case "org.freedesktop.DBus.Error.ServiceUnknown":
 *                 System.err.println("Service not available");
 *                 break;
 *             case "org.freedesktop.DBus.Error.AccessDenied":
 *                 System.err.println("Access denied");
 *                 break;
 *             default:
 *                 System.err.println("Method failed: " + errorName);
 *                 if (!error.getBody().isEmpty()) {
 *                     System.err.println("Error details: " + error.getBody().get(0));
 *                 }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Message Structure</h2>
 *
 * <p>All D-Bus messages share a common structure:
 *
 * <ul>
 *   <li><strong>Header:</strong> Contains message type, flags, and routing information
 *   <li><strong>Body:</strong> Contains typed arguments or return values
 *   <li><strong>Serial:</strong> Unique identifier for request/response matching
 *   <li><strong>Sender:</strong> Unique connection name of the message sender
 * </ul>
 *
 * <h2>Message Routing</h2>
 *
 * <p>D-Bus messages are routed based on several key fields:
 *
 * <ul>
 *   <li><strong>Destination:</strong> Target service name (for method calls)
 *   <li><strong>Object Path:</strong> Target object within the service
 *   <li><strong>Interface:</strong> Specific interface on the object
 *   <li><strong>Member:</strong> Method name or signal name
 * </ul>
 *
 * <h2>Asynchronous Processing</h2>
 *
 * <p>All message operations are asynchronous and return {@link
 * java.util.concurrent.CompletionStage}:
 *
 * <ul>
 *   <li>Method calls return futures that complete with method returns or errors
 *   <li>Signal handling is event-driven through the handler pipeline
 *   <li>Message processing never blocks the calling thread
 *   <li>Timeouts and cancellation are supported through CompletionStage
 * </ul>
 *
 * <h2>Type Safety</h2>
 *
 * <p>Message bodies use the type-safe D-Bus type system:
 *
 * <ul>
 *   <li>All arguments and return values are strongly typed
 *   <li>Type mismatches are caught at compile time when possible
 *   <li>Runtime validation ensures D-Bus specification compliance
 *   <li>Automatic marshalling/unmarshalling of complex types
 * </ul>
 *
 * @see InboundMessage
 * @see OutboundMessage
 * @see InboundMethodReturn
 * @see InboundError
 * @see InboundSignal
 * @see OutboundMethodCall
 * @see com.lucimber.dbus.type
 * @since 1.0
 */
package com.lucimber.dbus.message;
