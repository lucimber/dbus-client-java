/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Spring Boot Integration Example
 *
 * <p>This example demonstrates how to integrate D-Bus Client Java with Spring Boot applications.
 * While this example doesn't use actual Spring Boot annotations (to avoid dependencies), it shows
 * the patterns and structure you would use in a real Spring Boot application.
 *
 * <p>Key Integration Patterns: - Configuration beans for D-Bus connections - Service classes that
 * encapsulate D-Bus operations - Event-driven architecture using D-Bus signals - Health checks and
 * lifecycle management - Dependency injection patterns
 */
public class SpringBootIntegrationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Spring Boot Integration Example ===\n");

        // Simulate Spring Boot application startup
        SpringBootIntegrationExample example = new SpringBootIntegrationExample();
        example.runExample();
    }

    public void runExample() throws Exception {
        try {
            // 1. Demonstrate Configuration Pattern
            demonstrateConfigurationPattern();

            // 2. Demonstrate Service Pattern
            demonstrateServicePattern();

            // 3. Demonstrate Event-Driven Pattern
            demonstrateEventDrivenPattern();

            // 4. Demonstrate Health Check Pattern
            demonstrateHealthCheckPattern();

            System.out.println("✅ Spring Boot integration patterns demonstrated successfully!");

        } catch (Exception e) {
            System.err.println("❌ Spring Boot integration example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Demonstrates configuration pattern - how you would configure D-Bus in Spring Boot */
    private void demonstrateConfigurationPattern() {
        System.out.println("1. Configuration Pattern (Spring Boot @Configuration equivalent):");
        System.out.println(
                """
            // In a real Spring Boot app, you would use:

            @Configuration
            public class DBusConfiguration {

                @Bean
                @ConditionalOnProperty(name = "dbus.enabled", havingValue = "true")
                public Connection dbusConnection(
                    @Value("${dbus.connect-timeout:10s}") Duration connectTimeout,
                    @Value("${dbus.method-timeout:30s}") Duration methodTimeout) {

                    ConnectionConfig config = ConnectionConfig.builder()
                        .withConnectTimeout(connectTimeout)
                        .withMethodCallTimeout(methodTimeout)
                        .withAutoReconnectEnabled(true)
                        .withHealthCheckEnabled(true)
                        .build();

                    return NettyConnection.newSessionBusConnection(config);
                }

                @Bean
                public DBusService dbusService(Connection connection) {
                    return new DBusService(connection);
                }
            }
            """);
        System.out.println("✅ Configuration pattern explained\n");
    }

    /** Demonstrates service pattern - encapsulating D-Bus operations in services */
    private void demonstrateServicePattern() throws Exception {
        System.out.println("2. Service Pattern (Spring Boot @Service equivalent):");

        // Create a mock service class that would use @Service annotation
        DBusService service = new DBusService();
        service.initialize();

        // Demonstrate service operations
        service.listBusNames();
        service.getBusId();

        service.cleanup();
        System.out.println("✅ Service pattern demonstrated\n");
    }

    /** Demonstrates event-driven pattern using D-Bus signals */
    private void demonstrateEventDrivenPattern() throws Exception {
        System.out.println("3. Event-Driven Pattern (Spring Boot @EventListener equivalent):");

        // Create event-driven service
        EventDrivenService eventService = new EventDrivenService();
        eventService.initialize();

        System.out.println(
                "   Event listener would handle D-Bus signals and publish Spring events");
        System.out.println(
                "   Real implementation would use @EventListener and ApplicationEventPublisher");

        eventService.cleanup();
        System.out.println("✅ Event-driven pattern demonstrated\n");
    }

    /** Demonstrates health check pattern for monitoring */
    private void demonstrateHealthCheckPattern() {
        System.out.println("4. Health Check Pattern (Spring Boot Actuator integration):");
        System.out.println(
                """
            // In a real Spring Boot app with Actuator, you would use:

            @Component
            public class DBusHealthIndicator implements HealthIndicator {

                private final Connection dbusConnection;

                public DBusHealthIndicator(Connection dbusConnection) {
                    this.dbusConnection = dbusConnection;
                }

                @Override
                public Health health() {
                    try {
                        if (dbusConnection.isConnected()) {
                            return Health.up()
                                .withDetail("status", "connected")
                                .withDetail("bus-type", "session")
                                .build();
                        } else {
                            return Health.down()
                                .withDetail("status", "disconnected")
                                .build();
                        }
                    } catch (Exception e) {
                        return Health.down(e)
                            .withDetail("error", e.getMessage())
                            .build();
                    }
                }
            }
            """);
        System.out.println("✅ Health check pattern explained\n");
    }

    /** Mock service class demonstrating Spring Boot service patterns */
    static class DBusService {
        private Connection connection;

        // In Spring Boot: @Autowired constructor injection
        public DBusService() {
            // Constructor would receive injected Connection bean
        }

        // In Spring Boot: @PostConstruct
        public void initialize() throws Exception {
            System.out.println("   🔧 Initializing D-Bus service...");

            ConnectionConfig config =
                    ConnectionConfig.builder()
                            .withConnectTimeout(Duration.ofSeconds(5))
                            .withMethodCallTimeout(Duration.ofSeconds(10))
                            .withAutoReconnectEnabled(true)
                            .build();

            connection = NettyConnection.newSessionBusConnection(config);
            connection.connect().toCompletableFuture().get();

            System.out.println("   ✅ D-Bus service initialized");
        }

        public CompletableFuture<String> getBusId() {
            System.out.println("   📡 Getting bus ID...");

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
                    .thenApply(
                            response -> {
                                System.out.println("   ✅ Bus ID retrieved");
                                return response.toString();
                            })
                    .toCompletableFuture();
        }

        public CompletableFuture<Void> listBusNames() {
            System.out.println("   📋 Listing bus names...");

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
                    .thenAccept(
                            response -> {
                                System.out.println("   ✅ Bus names listed");
                            })
                    .toCompletableFuture();
        }

        // In Spring Boot: @PreDestroy
        public void cleanup() {
            System.out.println("   🧹 Cleaning up D-Bus service...");
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    System.err.println("   ❌ Error closing connection: " + e.getMessage());
                }
            }
            System.out.println("   ✅ D-Bus service cleaned up");
        }
    }

    /** Mock event-driven service demonstrating Spring event integration */
    static class EventDrivenService {
        private Connection connection;

        public void initialize() throws Exception {
            System.out.println("   🎯 Initializing event-driven service...");

            connection = NettyConnection.newSessionBusConnection();
            connection.connect().toCompletableFuture().get();

            // Add signal handler that would publish Spring events
            connection.getPipeline().addLast("spring-event-publisher", new SpringEventHandler());

            System.out.println("   ✅ Event-driven service initialized");
        }

        public void cleanup() {
            System.out.println("   🧹 Cleaning up event-driven service...");
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    System.err.println("   ❌ Error closing connection: " + e.getMessage());
                }
            }
            System.out.println("   ✅ Event-driven service cleaned up");
        }

        /** Handler that would publish Spring application events */
        static class SpringEventHandler extends AbstractInboundHandler {
            private static final org.slf4j.Logger logger =
                    org.slf4j.LoggerFactory.getLogger(SpringEventHandler.class);

            // In Spring Boot: @Autowired ApplicationEventPublisher
            // private ApplicationEventPublisher eventPublisher;

            @Override
            protected org.slf4j.Logger getLogger() {
                return logger;
            }

            @Override
            public void handleInboundMessage(Context ctx, InboundMessage msg) {
                if (msg instanceof InboundSignal) {
                    InboundSignal signal = (InboundSignal) msg;

                    System.out.println(
                            "   📢 Would publish Spring event for signal: " + signal.getMember());

                    // In real Spring Boot app:
                    // DBusSignalEvent event = new DBusSignalEvent(signal);
                    // eventPublisher.publishEvent(event);
                }

                ctx.propagateInboundMessage(msg);
            }
        }
    }
}
