/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.annotation.DBusInterface;
import com.lucimber.dbus.annotation.DBusMethod;
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.proxy.ServiceProxy;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.util.DBusPromise;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * High-Level Abstraction Example
 *
 * <p>This example demonstrates the new high-level abstractions that simplify D-Bus programming with
 * service proxies and promise-style async handling.
 */
public class HighLevelAbstractionExample {

    // Define a Java interface for the D-Bus service
    @DBusInterface("org.freedesktop.DBus")
    public interface DBusService {
        @DBusMethod(name = "GetId")
        String getId();

        @DBusMethod(name = "ListNames")
        CompletableFuture<String[]> listNames();

        @DBusMethod(name = "GetNameOwner")
        CompletableFuture<String> getNameOwner(String name);
    }

    // Example custom service interface
    @DBusInterface("com.example.Calculator")
    public interface CalculatorService {
        @DBusMethod(name = "Add")
        int add(int a, int b);

        @DBusMethod(name = "AddAsync")
        CompletableFuture<Integer> addAsync(int a, int b);

        @DBusMethod(name = "Divide")
        double divide(double a, double b);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== High-Level D-Bus Abstraction Example ===\n");

        Connection connection = null;

        try {
            // Connect to D-Bus
            connection = NettyConnection.newSystemBusConnection();
            connection.connect().toCompletableFuture().get();

            // Demonstrate service proxy
            demonstrateServiceProxy(connection);

            // Demonstrate promise-style operations
            demonstratePromises(connection);

            System.out.println("\n✅ All examples completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Example failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static void demonstrateServiceProxy(Connection connection) throws Exception {
        System.out.println("1. Service Proxy Pattern:");
        System.out.println("------------------------");

        // Create a proxy for the D-Bus service
        DBusService dbusService =
                ServiceProxy.create(
                        connection,
                        "org.freedesktop.DBus",
                        "/org/freedesktop/DBus",
                        DBusService.class);

        // Synchronous method call - as simple as calling a Java method!
        System.out.println("Synchronous call:");
        String id = dbusService.getId();
        System.out.println("  D-Bus ID: " + id);

        // Asynchronous method call
        System.out.println("\nAsynchronous call:");
        dbusService
                .listNames()
                .thenAccept(
                        names -> {
                            System.out.println("  Found " + names.length + " services");
                            // Show first 5 services
                            int count = Math.min(5, names.length);
                            for (int i = 0; i < count; i++) {
                                System.out.println("    - " + names[i]);
                            }
                            if (names.length > 5) {
                                System.out.println("    ... and " + (names.length - 5) + " more");
                            }
                        })
                .exceptionally(
                        error -> {
                            System.err.println("  Failed to list names: " + error.getMessage());
                            return null;
                        })
                .toCompletableFuture()
                .get(); // Wait for completion in this example

        System.out.println();
    }

    private static void demonstratePromises(Connection connection) throws Exception {
        System.out.println("2. Promise-Style Operations:");
        System.out.println("---------------------------");

        // Example 1: Simple promise with timeout
        System.out.println("Promise with timeout:");
        OutboundMethodCall getIdCall =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                        .withMember(DBusString.valueOf("GetId"))
                        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                        .withReplyExpected(true)
                        .build();

        DBusPromise.from(connection.sendRequest(getIdCall))
                .timeout(Duration.ofSeconds(5))
                .mapReturn()
                .firstAs(DBusString.class)
                .map(dbusString -> dbusString.getDelegate())
                .thenAccept(id -> System.out.println("  Got ID: " + id))
                .exceptionally(
                        error -> {
                            System.err.println("  Failed: " + error.getMessage());
                            return null;
                        })
                .get(); // Wait for completion

        // Example 2: Chained operations
        System.out.println("\nChained promise operations:");
        OutboundMethodCall listNamesCall =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                        .withMember(DBusString.valueOf("ListNames"))
                        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                        .withReplyExpected(true)
                        .build();

        DBusPromise.from(connection.sendRequest(listNamesCall))
                .timeout(Duration.ofSeconds(10))
                .mapReturn()
                .firstAs(DBusArray.class)
                .map(
                        array -> {
                            System.out.println("  Processing " + array.size() + " service names");
                            return array.size();
                        })
                .thenAccept(count -> System.out.println("  Total services: " + count))
                .get(); // Wait for completion

        // Example 3: Error handling
        System.out.println("\nError handling with promises:");
        OutboundMethodCall invalidCall =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf("/invalid/path"))
                        .withInterface(DBusString.valueOf("com.invalid.Interface"))
                        .withMember(DBusString.valueOf("InvalidMethod"))
                        .withDestination(DBusString.valueOf("com.invalid.Service"))
                        .withReplyExpected(true)
                        .build();

        DBusPromise.from(connection.sendRequest(invalidCall))
                .timeout(Duration.ofSeconds(2))
                .mapReturn()
                .thenAccept(result -> System.out.println("  Unexpected success"))
                .exceptionally(
                        error -> {
                            System.out.println(
                                    "  ✅ Error handled gracefully: " + error.getMessage());
                            return null;
                        })
                .get(); // Wait for completion

        System.out.println();
    }
}
