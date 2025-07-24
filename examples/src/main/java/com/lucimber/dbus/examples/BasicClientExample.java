/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusInt32;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic D-Bus Client Example
 *
 * <p>This example demonstrates fundamental D-Bus operations including: - Connection establishment
 * and management - Synchronous and asynchronous method calls - Working with D-Bus types - Error
 * handling patterns - Resource cleanup
 */
public class BasicClientExample {

    private static final String DBUS_SERVICE = "org.freedesktop.DBus";
    private static final String DBUS_PATH = "/org/freedesktop/DBus";
    private static final String DBUS_INTERFACE = "org.freedesktop.DBus";

    public static void main(String[] args) throws Exception {
        System.out.println("=== D-Bus Basic Client Example ===\n");

        BasicClientExample example = new BasicClientExample();
        example.runExample();
    }

    public void runExample() throws Exception {
        Connection connection = null;

        try {
            // 1. Connection Management
            connection = demonstrateConnectionManagement();

            // 2. Basic Method Calls
            demonstrateBasicMethodCalls(connection);

            // 3. Asynchronous Operations
            demonstrateAsyncOperations(connection);

            // 4. Working with D-Bus Types
            demonstrateDBusTypes();

            // 5. Error Handling
            demonstrateErrorHandling(connection);

            System.out.println("✅ All examples completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Example failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Always clean up resources
            if (connection != null) {
                connection.close();
                System.out.println("✅ Connection closed successfully");
            }
        }
    }

    /** Demonstrates connection establishment with different configurations */
    private Connection demonstrateConnectionManagement() throws Exception {
        System.out.println("1. Connection Management:");

        // Check required environment variables for D-Bus connection
        validateEnvironmentVariables();

        // Determine bus type from environment or use system bus as default
        String busType = System.getenv().getOrDefault("DBUS_BUS_TYPE", "SYSTEM");

        // Parse timeout configurations
        Duration connectTimeout =
                Duration.ofSeconds(
                        Integer.parseInt(
                                System.getenv().getOrDefault("DBUS_CONNECT_TIMEOUT", "10")));
        Duration methodTimeout =
                Duration.ofSeconds(
                        Integer.parseInt(
                                System.getenv().getOrDefault("DBUS_METHOD_TIMEOUT", "30")));

        // Create connection configuration
        ConnectionConfig config =
                ConnectionConfig.builder()
                        .withConnectTimeout(connectTimeout)
                        .withMethodCallTimeout(methodTimeout)
                        .withAutoReconnectEnabled(true)
                        .withReconnectInitialDelay(Duration.ofSeconds(1))
                        .withMaxReconnectAttempts(3)
                        .withHealthCheckEnabled(true)
                        .withHealthCheckInterval(Duration.ofSeconds(30))
                        .build();

        // Create connection based on bus type
        Connection connection =
                "SESSION".equalsIgnoreCase(busType)
                        ? NettyConnection.newSessionBusConnection(config)
                        : NettyConnection.newSystemBusConnection(config);

        // Connect to D-Bus
        System.out.println("🔗 Connecting to D-Bus " + busType.toLowerCase() + " bus...");
        connection
                .connect()
                .toCompletableFuture()
                .get(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);

        // Verify connection
        if (connection.isConnected()) {
            System.out.println("✅ Connected to D-Bus " + busType.toLowerCase() + " bus");
            System.out.println("✅ Connection status: active");
            System.out.println("✅ Transport: " + connection.getClass().getSimpleName());
        } else {
            throw new RuntimeException("Failed to establish connection");
        }

        System.out.println();
        return connection;
    }

    /** Validates required environment variables for D-Bus SASL authentication */
    private void validateEnvironmentVariables() {
        System.out.println("🔍 Checking D-Bus environment variables...");

        // Check session bus address (required for session bus connections)
        String sessionBusAddress = System.getenv("DBUS_SESSION_BUS_ADDRESS");
        if (sessionBusAddress != null) {
            System.out.println("   ✅ DBUS_SESSION_BUS_ADDRESS: " + sessionBusAddress);
        } else {
            System.out.println(
                    "   ⚠️  DBUS_SESSION_BUS_ADDRESS: not set (required for session bus)");
        }

        // Check system bus address (optional, has default)
        String systemBusAddress = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
        if (systemBusAddress != null) {
            System.out.println("   ✅ DBUS_SYSTEM_BUS_ADDRESS: " + systemBusAddress);
        } else {
            System.out.println(
                    "   ℹ️  DBUS_SYSTEM_BUS_ADDRESS: not set (using default: /var/run/dbus/system_bus_socket)");
        }

        // Check system properties required for SASL authentication
        String userName = System.getProperty("user.name");
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name");

        System.out.println("   ✅ user.name: " + userName + " (required for DBUS_COOKIE_SHA1)");
        System.out.println("   ✅ user.home: " + userHome + " (required for DBUS_COOKIE_SHA1)");
        System.out.println("   ✅ os.name: " + osName + " (required for EXTERNAL mechanism)");

        // Check for D-Bus keyring directory (used by DBUS_COOKIE_SHA1)
        if (userHome != null) {
            java.io.File keyringDir = new java.io.File(userHome, ".dbus-keyrings");
            if (keyringDir.exists()) {
                System.out.println("   ✅ D-Bus keyring directory: " + keyringDir.getAbsolutePath());
            } else {
                System.out.println(
                        "   ⚠️  D-Bus keyring directory not found: "
                                + keyringDir.getAbsolutePath()
                                + " (needed for DBUS_COOKIE_SHA1)");
            }
        }

        System.out.println();
    }

    /** Demonstrates basic synchronous method calls */
    private void demonstrateBasicMethodCalls(Connection connection) throws Exception {
        System.out.println("2. Basic Method Calls:");

        // Example 1: Get D-Bus daemon ID
        System.out.println("📞 Calling " + DBUS_INTERFACE + ".GetId()");

        OutboundMethodCall getIdCall =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf(DBUS_PATH))
                        .withInterface(DBusString.valueOf(DBUS_INTERFACE))
                        .withMember(DBusString.valueOf("GetId"))
                        .withDestination(DBusString.valueOf(DBUS_SERVICE))
                        .withReplyExpected(true)
                        .build();

        InboundMessage idResponse =
                connection.sendRequest(getIdCall).toCompletableFuture().get(10, TimeUnit.SECONDS);

        if (idResponse instanceof InboundMethodReturn methodReturn) {
            System.out.println("✅ D-Bus daemon ID received");

            // Parse the response payload to extract the daemon ID
            List<DBusType> payload = methodReturn.getPayload();
            if (!payload.isEmpty() && payload.get(0) instanceof DBusString) {
                String daemonId = ((DBusString) payload.get(0)).getDelegate();
                System.out.println("   └─ Daemon ID: " + daemonId);
                System.out.println("   └─ ID Length: " + daemonId.length() + " characters");
                System.out.println(
                        "   └─ ID Format: "
                                + (daemonId.matches("[0-9a-fA-F]+") ? "Hexadecimal" : "Mixed"));
            } else {
                System.out.println("   └─ ⚠️  Could not parse daemon ID from response");
            }
        } else {
            System.out.println(
                    "❌ Unexpected response type: " + idResponse.getClass().getSimpleName());
        }

        // Example 2: List available services
        System.out.println("📞 Calling " + DBUS_INTERFACE + ".ListNames()");

        OutboundMethodCall listNamesCall =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf(DBUS_PATH))
                        .withInterface(DBusString.valueOf(DBUS_INTERFACE))
                        .withMember(DBusString.valueOf("ListNames"))
                        .withDestination(DBusString.valueOf(DBUS_SERVICE))
                        .withReplyExpected(true)
                        .build();

        InboundMessage namesResponse =
                connection
                        .sendRequest(listNamesCall)
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);

        if (namesResponse instanceof InboundMethodReturn methodReturn) {
            System.out.println("✅ Service list received");

            // Parse the response payload to extract the service names
            List<DBusType> payload = methodReturn.getPayload();
            if (!payload.isEmpty() && payload.get(0) instanceof DBusArray) {
                @SuppressWarnings("unchecked")
                DBusArray<DBusString> serviceArray = (DBusArray<DBusString>) payload.get(0);

                System.out.println("   └─ Total services: " + serviceArray.size());

                // Count different types of services
                int wellKnownNames = 0;
                int uniqueNames = 0;

                System.out.println("   └─ Sample services:");
                int count = 0;
                for (DBusString serviceString : serviceArray) {
                    String serviceName = serviceString.getDelegate();

                    if (serviceName.startsWith(":")) {
                        uniqueNames++;
                    } else {
                        wellKnownNames++;
                    }

                    // Show first 5 services as examples
                    if (count < 5) {
                        System.out.println("       • " + serviceName);
                        count++;
                    }
                }

                if (serviceArray.size() > 5) {
                    System.out.println("       ... and " + (serviceArray.size() - 5) + " more");
                }

                System.out.println("   └─ Well-known names: " + wellKnownNames);
                System.out.println("   └─ Unique names: " + uniqueNames);
            } else {
                System.out.println("   └─ ⚠️  Could not parse service list from response");
            }
        }

        System.out.println();
    }

    /** Demonstrates asynchronous method call patterns */
    private void demonstrateAsyncOperations(Connection connection) throws Exception {
        System.out.println("3. Asynchronous Operations:");

        System.out.println("🔄 Starting async method call...");

        OutboundMethodCall asyncCall =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf(DBUS_PATH))
                        .withInterface(DBusString.valueOf(DBUS_INTERFACE))
                        .withMember(DBusString.valueOf("GetId"))
                        .withDestination(DBusString.valueOf(DBUS_SERVICE))
                        .withReplyExpected(true)
                        .build();

        // Send request asynchronously
        CompletionStage<InboundMessage> completionStage = connection.sendRequest(asyncCall);

        // Process response asynchronously
        CompletableFuture<String> result =
                completionStage
                        .thenApply(
                                response -> {
                                    if (response instanceof InboundMethodReturn) {
                                        return "success";
                                    } else if (response instanceof InboundError error) {
                                        return "error: " + error.getErrorName();
                                    } else {
                                        return "unknown response type";
                                    }
                                })
                        .exceptionally(throwable -> "exception: " + throwable.getMessage())
                        .toCompletableFuture();

        // Wait for result
        String asyncResult = result.get(10, TimeUnit.SECONDS);
        System.out.println("✅ Async result received: " + asyncResult);

        System.out.println();
    }

    /** Demonstrates working with D-Bus types */
    private void demonstrateDBusTypes() {
        System.out.println("4. Working with D-Bus Types:");

        // Basic types
        DBusString text = DBusString.valueOf("Hello, D-Bus!");
        DBusInt32 number = DBusInt32.valueOf(42);

        System.out.println("✅ Created DBusString: \"" + text + "\"");
        System.out.println("✅ Created DBusInt32: " + number);

        // Container types - Array
        DBusSignature arraySignature = DBusSignature.valueOf("as");
        DBusArray<DBusString> stringArray = new DBusArray<>(arraySignature);
        stringArray.add(DBusString.valueOf("first"));
        stringArray.add(DBusString.valueOf("second"));
        stringArray.add(DBusString.valueOf("third"));

        System.out.println("✅ Created DBusArray with " + stringArray.size() + " elements");

        // Demonstrate type safety
        try {
            // This would cause a compilation error due to type safety:
            // stringArray.add(DBusInt32.valueOf(123)); // Compilation error!

            System.out.println("✅ Type safety enforced at compile time");
        } catch (Exception e) {
            System.out.println("❌ Type safety violation: " + e.getMessage());
        }

        System.out.println();
    }

    /** Demonstrates error handling patterns */
    private void demonstrateErrorHandling(Connection connection) {
        System.out.println("5. Error Handling:");

        // Example 1: Handle method call that returns an error
        try {
            OutboundMethodCall errorCall =
                    OutboundMethodCall.Builder.create()
                            .withPath(DBusObjectPath.valueOf("/nonexistent/path"))
                            .withInterface(DBusString.valueOf("com.example.NonExistent"))
                            .withMember(DBusString.valueOf("NonExistentMethod"))
                            .withDestination(DBusString.valueOf("com.example.NonExistentService"))
                            .withReplyExpected(true)
                            .build();

            InboundMessage response =
                    connection
                            .sendRequest(errorCall)
                            .toCompletableFuture()
                            .get(5, TimeUnit.SECONDS);

            if (response instanceof InboundError error) {
                String errorName = error.getErrorName().toString();
                System.out.println("❌ Expected error: " + getErrorDescription(errorName));
                System.out.println("✅ Error handled gracefully");
            } else {
                System.out.println(
                        "⚠️ Expected error response, got: " + response.getClass().getSimpleName());
            }

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TimeoutException) {
                System.out.println("❌ Method call timed out (expected for non-existent service)");
                System.out.println("✅ Timeout handled gracefully");
            } else {
                System.out.println("❌ Unexpected error: " + cause.getMessage());
                System.out.println("✅ Exception handled gracefully");
            }
        } catch (Exception e) {
            System.out.println("❌ Error: " + e.getMessage());
            System.out.println("✅ Exception handled gracefully");
        }

        System.out.println();
    }

    /** Convert D-Bus error names to human-readable descriptions */
    private String getErrorDescription(String errorName) {
        return switch (errorName) {
            case "org.freedesktop.DBus.Error.ServiceUnknown" -> "Service not found";
            case "org.freedesktop.DBus.Error.AccessDenied" -> "Access denied";
            case "org.freedesktop.DBus.Error.InvalidArgs" -> "Invalid arguments";
            case "org.freedesktop.DBus.Error.NoReply" -> "No reply from service";
            case "org.freedesktop.DBus.Error.Timeout" -> "Operation timed out";
            default -> errorName;
        };
    }
}
