/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple Reactive Programming Example
 *
 * <p>This example demonstrates basic reactive programming patterns with D-Bus Client Java, focusing
 * on CompletableFuture composition, async operations, and error handling.
 *
 * <p>Key Patterns Demonstrated: - CompletableFuture chaining and composition - Async method call
 * pipelines - Error recovery with fallbacks - Timeout handling - Resource cleanup
 */
public class SimpleReactiveExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Simple Reactive Programming Example ===\n");

        SimpleReactiveExample example = new SimpleReactiveExample();
        example.runExample();
    }

    public void runExample() throws Exception {
        try {
            // 1. Demonstrate Async Composition
            demonstrateAsyncComposition();

            // 2. Demonstrate Error Recovery
            demonstrateErrorRecovery();

            // 3. Demonstrate Timeout Handling
            demonstrateTimeoutHandling();

            System.out.println("✅ All reactive patterns demonstrated successfully!");

        } catch (Exception e) {
            System.err.println("❌ Reactive example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Demonstrates CompletableFuture composition for async operations */
    private void demonstrateAsyncComposition() throws Exception {
        System.out.println("1. Async Composition:\n");

        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withMethodCallTimeout(Duration.ofSeconds(10))
                        .build();

        Connection connection = NettyConnection.newSessionBusConnection(config);

        try {
            // Chain multiple async operations
            CompletableFuture<String> result =
                    connection
                            .connect()
                            .thenCompose(
                                    v -> {
                                        System.out.println("   🔗 Connected to D-Bus");
                                        return getBusId(connection);
                                    })
                            .thenCompose(
                                    busId -> {
                                        System.out.println(
                                                "   🆔 Got bus ID: "
                                                        + busId.substring(
                                                                0, Math.min(8, busId.length()))
                                                        + "...");
                                        return getServiceCount(connection);
                                    })
                            .thenApply(
                                    count -> {
                                        System.out.println("   📋 Found " + count + " services");
                                        return "Pipeline completed with " + count + " services";
                                    })
                            .exceptionally(
                                    throwable -> {
                                        System.err.println(
                                                "   ❌ Pipeline failed: " + throwable.getMessage());
                                        return "Pipeline failed";
                                    })
                            .toCompletableFuture();

            String finalResult = result.get(15, TimeUnit.SECONDS);
            System.out.println("   ✅ " + finalResult);

        } finally {
            connection.close();
        }

        System.out.println();
    }

    /** Demonstrates error recovery patterns */
    private void demonstrateErrorRecovery() throws Exception {
        System.out.println("2. Error Recovery:\n");

        Connection connection = NettyConnection.newSessionBusConnection();

        try {
            connection.connect().toCompletableFuture().get(5, TimeUnit.SECONDS);

            // Demonstrate retry with fallback
            CompletableFuture<String> withFallback =
                    performRiskyOperation(connection)
                            .exceptionally(
                                    error -> {
                                        System.out.println(
                                                "   ⚠️ Primary operation failed, using fallback");
                                        return "fallback-result";
                                    })
                            .thenApply(
                                    result -> {
                                        System.out.println("   ✅ Final result: " + result);
                                        return result;
                                    });

            withFallback.get(10, TimeUnit.SECONDS);

        } finally {
            connection.close();
        }

        System.out.println();
    }

    /** Demonstrates timeout handling */
    private void demonstrateTimeoutHandling() throws Exception {
        System.out.println("3. Timeout Handling:\n");

        Connection connection = NettyConnection.newSessionBusConnection();

        try {
            connection.connect().toCompletableFuture().get(5, TimeUnit.SECONDS);

            // Demonstrate timeout with cleanup
            CompletableFuture<String> withTimeout =
                    getBusId(connection)
                            .orTimeout(2, TimeUnit.SECONDS)
                            .handle(
                                    (result, error) -> {
                                        if (error != null) {
                                            System.out.println(
                                                    "   ⏰ Operation timed out, cleaning up...");
                                            return "timeout-fallback";
                                        } else {
                                            System.out.println(
                                                    "   ✅ Operation completed within timeout");
                                            return result;
                                        }
                                    });

            String result = withTimeout.get(5, TimeUnit.SECONDS);
            System.out.println("   📋 Final result: " + result);

        } finally {
            connection.close();
        }

        System.out.println();
    }

    // Helper methods for async operations
    private CompletableFuture<String> getBusId(Connection connection) {
        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                        .withMember(DBusString.valueOf("GetId"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                        .withReplyExpected(true)
                        .build();

        return connection
                .sendRequest(call)
                .thenApply(response -> response.toString())
                .toCompletableFuture();
    }

    private CompletableFuture<Integer> getServiceCount(Connection connection) {
        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                        .withMember(DBusString.valueOf("ListNames"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                        .withReplyExpected(true)
                        .build();

        return connection
                .sendRequest(call)
                .thenApply(response -> 42) // Mock service count
                .toCompletableFuture();
    }

    private CompletableFuture<String> performRiskyOperation(Connection connection) {
        // Simulate an operation that might fail
        return CompletableFuture.supplyAsync(
                () -> {
                    if (Math.random() > 0.7) {
                        throw new RuntimeException("Simulated failure");
                    }
                    return "success-result";
                });
    }
}
