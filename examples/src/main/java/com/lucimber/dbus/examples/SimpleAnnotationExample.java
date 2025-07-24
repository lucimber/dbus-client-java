/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.annotation.DBusInterface;
import com.lucimber.dbus.annotation.DBusProperty;
import com.lucimber.dbus.annotation.StandardInterfaceHandler;
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusUInt32;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple example demonstrating annotation-based D-Bus service implementation.
 *
 * <p>This example shows how to: 1. Create a D-Bus service using annotations 2. Register it with the
 * StandardInterfaceHandler 3. Test the service using D-Bus tools
 */
public class SimpleAnnotationExample {

    private static final String SERVICE_NAME = "com.example.SimpleService";
    private static final String OBJECT_PATH = "/com/example/Simple";

    /** Simple calculator service using annotations. */
    @DBusInterface("com.example.Calculator")
    public static class SimpleCalculator {

        @DBusProperty(name = "Version")
        private final String version = "1.0.0";

        @DBusProperty(name = "Author")
        private String author = "D-Bus Client Java";

        @DBusProperty(name = "OperationCount")
        private final AtomicInteger operationCount = new AtomicInteger(0);

        @DBusProperty(name = "LastResult")
        private double lastResult = 0.0;

        // These would be callable via custom method implementation in the future
        public double add(double a, double b) {
            operationCount.incrementAndGet();
            lastResult = a + b;
            return lastResult;
        }

        public double multiply(double a, double b) {
            operationCount.incrementAndGet();
            lastResult = a * b;
            return lastResult;
        }

        public void reset() {
            operationCount.set(0);
            lastResult = 0.0;
        }
    }

    public static void main(String[] args) throws Exception {
        Connection connection = null;

        try {
            // Create connection configuration
            ConnectionConfig config =
                    ConnectionConfig.builder()
                            .withConnectTimeout(Duration.ofSeconds(10))
                            .withMethodCallTimeout(Duration.ofSeconds(30))
                            .build();

            // Connect to session bus
            connection = NettyConnection.newSessionBusConnection(config);
            connection.connect().toCompletableFuture().get(10, TimeUnit.SECONDS);

            // Request service name
            requestServiceName(connection, SERVICE_NAME);

            // Create and register the annotated service
            SimpleCalculator calculator = new SimpleCalculator();
            StandardInterfaceHandler handler =
                    new StandardInterfaceHandler(OBJECT_PATH, calculator);

            connection.getPipeline().addLast("simple-service", handler);

            System.out.println("🚀 Simple Annotation-based D-Bus Service Started");
            System.out.println("   Service Name: " + SERVICE_NAME);
            System.out.println("   Object Path:  " + OBJECT_PATH);
            System.out.println();
            System.out.println("📖 The service provides the following standard interfaces:");
            System.out.println("   • org.freedesktop.DBus.Introspectable");
            System.out.println("   • org.freedesktop.DBus.Properties");
            System.out.println("   • org.freedesktop.DBus.Peer");
            System.out.println("   • com.example.Calculator (custom interface with properties)");
            System.out.println();
            System.out.println("🔧 Test with these commands:");
            System.out.println();
            System.out.println("   # Introspect the service:");
            System.out.println("   dbus-send --session --print-reply \\");
            System.out.println("     --dest=" + SERVICE_NAME + " " + OBJECT_PATH + " \\");
            System.out.println("     org.freedesktop.DBus.Introspectable.Introspect");
            System.out.println();
            System.out.println("   # Get all properties:");
            System.out.println("   dbus-send --session --print-reply \\");
            System.out.println("     --dest=" + SERVICE_NAME + " " + OBJECT_PATH + " \\");
            System.out.println("     org.freedesktop.DBus.Properties.GetAll \\");
            System.out.println("     string:\"com.example.Calculator\"");
            System.out.println();
            System.out.println("   # Get specific property:");
            System.out.println("   dbus-send --session --print-reply \\");
            System.out.println("     --dest=" + SERVICE_NAME + " " + OBJECT_PATH + " \\");
            System.out.println("     org.freedesktop.DBus.Properties.Get \\");
            System.out.println("     string:\"com.example.Calculator\" string:\"Version\"");
            System.out.println();
            System.out.println("   # Test connectivity:");
            System.out.println("   dbus-send --session --print-reply \\");
            System.out.println("     --dest=" + SERVICE_NAME + " " + OBJECT_PATH + " \\");
            System.out.println("     org.freedesktop.DBus.Peer.Ping");
            System.out.println();
            System.out.println("Press Ctrl+C to stop the service...");

            // Keep running until interrupted
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            System.out.println("\n🛑 Service stopped by user");
        } catch (Exception e) {
            System.err.println("❌ Error running service: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    private static void requestServiceName(Connection connection, String serviceName)
            throws Exception {
        OutboundMethodCall requestName =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                        .withMember(DBusString.valueOf("RequestName"))
                        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                        .withBody(
                                DBusSignature.valueOf("su"),
                                Arrays.asList(
                                        DBusString.valueOf(serviceName),
                                        DBusUInt32.valueOf(0) // No flags
                                        ))
                        .withReplyExpected(true)
                        .build();

        connection.sendRequest(requestName).toCompletableFuture().get(5, TimeUnit.SECONDS);
        System.out.println("✅ Successfully registered service name: " + serviceName);
    }
}
