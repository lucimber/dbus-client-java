/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.connection.DummyConnection;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.type.DBusInt32;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example demonstrating unit testing with DummyConnection.
 *
 * <p>This example shows how to: - Use DummyConnection for unit testing without a real D-Bus daemon
 * - Test custom handlers in isolation - Simulate various D-Bus scenarios - Verify handler behavior
 * programmatically
 */
public class DummyConnectionTestingExample {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DummyConnectionTestingExample.class);

    public static void main(String[] args) throws Exception {
        System.out.println("🧪 D-Bus Unit Testing with DummyConnection Example");
        System.out.println("===================================================");

        // Example 1: Basic DummyConnection usage
        testBasicDummyConnection();

        // Example 2: Testing custom handlers
        testCustomHandlers();

        // Example 3: Testing error scenarios
        testErrorScenarios();

        System.out.println("🏁 All DummyConnection tests completed successfully!");
    }

    /** Demonstrates basic DummyConnection usage for testing. */
    private static void testBasicDummyConnection() throws Exception {
        System.out.println("\n📋 Test 1: Basic DummyConnection Usage");
        System.out.println("=====================================");

        // Create DummyConnection with basic configuration
        DummyConnection connection =
                DummyConnection.builder().withConnectDelay(Duration.ofMillis(10)).build();

        try {
            // Connect (always succeeds with DummyConnection)
            System.out.println("🔗 Connecting to DummyConnection...");
            connection.connect().toCompletableFuture().get(1, TimeUnit.SECONDS);
            System.out.println("✅ Connected successfully!");

            // Send a method call
            OutboundMethodCall call =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/example/Test"))
                            .withMember(DBusString.valueOf("GetVersion"))
                            .withDestination(DBusString.valueOf("org.example.Service"))
                            .withInterface(DBusString.valueOf("org.example.TestInterface"))
                            .withReplyExpected(true)
                            .build();

            System.out.println("📤 Sending method call: " + call.getMember().getDelegate());

            CompletableFuture<InboundMessage> response =
                    connection.sendRequest(call).toCompletableFuture();
            InboundMessage reply = response.get(1, TimeUnit.SECONDS);

            System.out.println("📥 Received reply: " + reply.getClass().getSimpleName());
            System.out.println("   Serial: " + reply.getSerial().getDelegate());

        } finally {
            connection.close();
            System.out.println("🔌 Connection closed");
        }
    }

    /** Demonstrates testing custom handlers with DummyConnection. */
    private static void testCustomHandlers() throws Exception {
        System.out.println("\n📋 Test 2: Testing Custom Handlers");
        System.out.println("==================================");

        // Create DummyConnection with basic configuration
        DummyConnection connection =
                DummyConnection.builder().withConnectDelay(Duration.ofMillis(10)).build();

        // Add our custom handler
        CalculatorHandler calculatorHandler = new CalculatorHandler();
        connection.getPipeline().addLast("calculator", calculatorHandler);

        try {
            connection.connect().toCompletableFuture().get(1, TimeUnit.SECONDS);

            // Test addition operation
            OutboundMethodCall addCall =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/com/example/Calculator"))
                            .withMember(DBusString.valueOf("Add"))
                            .withDestination(DBusString.valueOf("com.example.Calculator"))
                            .withInterface(DBusString.valueOf("com.example.Calculator"))
                            .withReplyExpected(true)
                            .withBody(
                                    DBusSignature.valueOf("ii"),
                                    List.of(DBusInt32.valueOf(15), DBusInt32.valueOf(25)))
                            .build();

            System.out.println("🧮 Testing calculator: Add(15, 25)");

            CompletableFuture<InboundMessage> response =
                    connection.sendRequest(addCall).toCompletableFuture();
            InboundMessage reply = response.get(1, TimeUnit.SECONDS);

            if (reply instanceof InboundMethodReturn) {
                InboundMethodReturn methodReturn = (InboundMethodReturn) reply;
                List<DBusType> payload = methodReturn.getPayload();
                if (!payload.isEmpty()) {
                    DBusInt32 result = (DBusInt32) payload.get(0);
                    System.out.println("✅ Result: " + result.getDelegate());

                    if (result.getDelegate().equals(40)) {
                        System.out.println("🎉 Calculator test passed!");
                    } else {
                        System.out.println(
                                "❌ Calculator test failed! Expected 40, got "
                                        + result.getDelegate());
                    }
                }
            }

        } finally {
            connection.close();
            System.out.println("🔌 Connection closed");
        }
    }

    /** Demonstrates testing error scenarios with DummyConnection. */
    private static void testErrorScenarios() throws Exception {
        System.out.println("\n📋 Test 3: Testing Error Scenarios");
        System.out.println("==================================");

        DummyConnection connection =
                DummyConnection.builder().withConnectDelay(Duration.ofMillis(10)).build();

        // Add handler that generates errors for certain inputs
        ErrorTestHandler errorHandler = new ErrorTestHandler();
        connection.getPipeline().addLast("error-test", errorHandler);

        try {
            connection.connect().toCompletableFuture().get(1, TimeUnit.SECONDS);

            // Test division by zero
            OutboundMethodCall divideCall =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/com/example/Calculator"))
                            .withMember(DBusString.valueOf("Divide"))
                            .withDestination(DBusString.valueOf("com.example.Calculator"))
                            .withInterface(DBusString.valueOf("com.example.Calculator"))
                            .withReplyExpected(true)
                            .withBody(
                                    DBusSignature.valueOf("ii"),
                                    List.of(DBusInt32.valueOf(10), DBusInt32.valueOf(0)))
                            .build();

            System.out.println("🧮 Testing error handling: Divide(10, 0)");

            CompletableFuture<InboundMessage> response =
                    connection.sendRequest(divideCall).toCompletableFuture();
            InboundMessage reply = response.get(1, TimeUnit.SECONDS);

            if (reply instanceof InboundError) {
                InboundError error = (InboundError) reply;
                System.out.println(
                        "✅ Received expected error: " + error.getErrorName().getDelegate());
                List<DBusType> payload = error.getPayload();
                if (!payload.isEmpty()) {
                    DBusString errorMessage = (DBusString) payload.get(0);
                    System.out.println("   Error message: " + errorMessage.getDelegate());
                }
                System.out.println("🎉 Error handling test passed!");
            } else {
                System.out.println(
                        "❌ Expected error response, got: " + reply.getClass().getSimpleName());
            }

        } finally {
            connection.close();
            System.out.println("🔌 Connection closed");
        }
    }

    /** Example handler that implements a simple calculator. */
    private static class CalculatorHandler extends AbstractInboundHandler {

        @Override
        protected Logger getLogger() {
            return LOGGER;
        }

        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundMethodCall) {
                InboundMethodCall methodCall = (InboundMethodCall) msg;
                String method = methodCall.getMember().getDelegate();

                if ("Add".equals(method)) {
                    handleAddition(ctx, methodCall);
                } else {
                    // Pass to next handler
                    ctx.propagateInboundMessage(msg);
                }
            } else {
                ctx.propagateInboundMessage(msg);
            }
        }

        private void handleAddition(Context ctx, InboundMethodCall call) {
            try {
                List<DBusType> payload = call.getPayload();
                if (payload.size() >= 2) {
                    DBusInt32 a = (DBusInt32) payload.get(0);
                    DBusInt32 b = (DBusInt32) payload.get(1);

                    int result = a.getDelegate() + b.getDelegate();

                    OutboundMethodReturn response =
                            OutboundMethodReturn.Builder.create()
                                    .withSerial(call.getSerial())
                                    .withReplySerial(call.getSerial())
                                    .withDestination(DBusString.valueOf("com.example.Calculator"))
                                    .withBody(
                                            DBusSignature.valueOf("i"),
                                            List.of(DBusInt32.valueOf(result)))
                                    .build();

                    ctx.propagateOutboundMessage(response, new CompletableFuture<>());
                    return;
                }

                // Invalid arguments
                OutboundError error =
                        OutboundError.Builder.create()
                                .withSerial(call.getSerial())
                                .withReplySerial(call.getSerial())
                                .withErrorName(
                                        DBusString.valueOf(
                                                "org.freedesktop.DBus.Error.InvalidArgs"))
                                .withBody(
                                        DBusSignature.valueOf("s"),
                                        List.of(
                                                DBusString.valueOf(
                                                        "Add requires two integer arguments")))
                                .build();

                ctx.propagateOutboundMessage(error, new CompletableFuture<>());

            } catch (Exception e) {
                getLogger().error("Error handling addition", e);

                OutboundError error =
                        OutboundError.Builder.create()
                                .withSerial(call.getSerial())
                                .withReplySerial(call.getSerial())
                                .withErrorName(
                                        DBusString.valueOf("org.freedesktop.DBus.Error.Failed"))
                                .withBody(
                                        DBusSignature.valueOf("s"),
                                        List.of(
                                                DBusString.valueOf(
                                                        "Internal error: " + e.getMessage())))
                                .build();

                ctx.propagateOutboundMessage(error, new CompletableFuture<>());
            }
        }
    }

    /** Example handler that demonstrates error responses. */
    private static class ErrorTestHandler extends AbstractInboundHandler {

        @Override
        protected Logger getLogger() {
            return LOGGER;
        }

        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundMethodCall) {
                InboundMethodCall methodCall = (InboundMethodCall) msg;
                String method = methodCall.getMember().getDelegate();

                if ("Divide".equals(method)) {
                    handleDivision(ctx, methodCall);
                } else {
                    ctx.propagateInboundMessage(msg);
                }
            } else {
                ctx.propagateInboundMessage(msg);
            }
        }

        private void handleDivision(Context ctx, InboundMethodCall call) {
            try {
                List<DBusType> payload = call.getPayload();
                if (payload.size() >= 2) {
                    DBusInt32 a = (DBusInt32) payload.get(0);
                    DBusInt32 b = (DBusInt32) payload.get(1);

                    if (b.getDelegate() == 0) {
                        // Division by zero error
                        OutboundError error =
                                OutboundError.Builder.create()
                                        .withSerial(call.getSerial())
                                        .withReplySerial(call.getSerial())
                                        .withErrorName(
                                                DBusString.valueOf(
                                                        "org.freedesktop.DBus.Error.InvalidArgs"))
                                        .withBody(
                                                DBusSignature.valueOf("s"),
                                                List.of(
                                                        DBusString.valueOf(
                                                                "Division by zero is not allowed")))
                                        .build();

                        ctx.propagateOutboundMessage(error, new CompletableFuture<>());
                        return;
                    }

                    int result = a.getDelegate() / b.getDelegate();

                    OutboundMethodReturn response =
                            OutboundMethodReturn.Builder.create()
                                    .withSerial(call.getSerial())
                                    .withReplySerial(call.getSerial())
                                    .withDestination(DBusString.valueOf("com.example.Calculator"))
                                    .withBody(
                                            DBusSignature.valueOf("i"),
                                            List.of(DBusInt32.valueOf(result)))
                                    .build();

                    ctx.propagateOutboundMessage(response, new CompletableFuture<>());
                    return;
                }

                // Invalid arguments
                OutboundError error =
                        OutboundError.Builder.create()
                                .withSerial(call.getSerial())
                                .withReplySerial(call.getSerial())
                                .withErrorName(
                                        DBusString.valueOf(
                                                "org.freedesktop.DBus.Error.InvalidArgs"))
                                .withBody(
                                        DBusSignature.valueOf("s"),
                                        List.of(
                                                DBusString.valueOf(
                                                        "Divide requires two integer arguments")))
                                .build();

                ctx.propagateOutboundMessage(error, new CompletableFuture<>());

            } catch (Exception e) {
                getLogger().error("Error handling division", e);
            }
        }
    }
}
