/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive example demonstrating D-Bus signal handling patterns.
 *
 * <p>This example shows how to: - Subscribe to D-Bus signals - Filter signals by interface and
 * member - Handle different signal types - Process signal arguments - Build event-driven
 * applications
 */
public class SignalHandlingExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(SignalHandlingExample.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) throws Exception {
        // Parse command line arguments
        Config config = parseArgs(args);

        if (config.showHelp) {
            showUsage();
            return;
        }

        System.out.println("=== D-Bus Signal Handling Example ===");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  D-Bus Address: " + config.dbusAddress);
        System.out.println(
                "  Interfaces: " + (config.interfaces.isEmpty() ? "ALL" : config.interfaces));
        System.out.println("  Timeout: " + config.timeout + "s");
        System.out.println("  Mode: " + config.mode);
        System.out.println();

        // Validate environment variables for D-Bus connection
        validateDbusEnvironment(config.dbusAddress);

        // Create connection configuration
        ConnectionConfig.Builder connectionConfigBuilder =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(30))
                        .withAutoReconnectEnabled(true);

        if (config.dbusAddress.contains("system")) {
            System.out.println("📡 Connecting to system bus...");
        } else {
            System.out.println("📡 Connecting to session bus...");
        }

        try (Connection connection =
                NettyConnection.newSystemBusConnection(connectionConfigBuilder.build())) {
            // Add signal handler to pipeline
            SignalHandler signalHandler = new SignalHandler(config.interfaces, config.mode);
            connection.getPipeline().addLast("signal-handler", signalHandler);

            // Connect to D-Bus
            connection.connect().toCompletableFuture().get(30, TimeUnit.SECONDS);
            System.out.println("✅ Connected to D-Bus");
            System.out.println();

            if (config.mode.equals("demo")) {
                runDemo(config.timeout);
            } else {
                System.out.println("🎯 Listening for signals... (Press Ctrl+C to stop)");
                System.out.println();

                // Set up graceful shutdown
                CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
                Runtime.getRuntime()
                        .addShutdownHook(
                                new Thread(
                                        () -> {
                                            System.out.println();
                                            System.out.println(
                                                    "🛑 Shutting down signal monitoring...");
                                            shutdownFuture.complete(null);
                                        }));

                // Wait for shutdown or timeout
                try {
                    shutdownFuture.get(config.timeout, TimeUnit.SECONDS);
                } catch (Exception e) {
                    System.out.println("⏰ Timeout reached, shutting down...");
                }
            }

        } finally {
            System.out.println("✅ Signal monitoring completed");
        }
    }

    private static void runDemo(int timeout) throws InterruptedException {
        System.out.println("🎬 Running in demo mode...");
        System.out.println("📨 Simulating signal events:");
        System.out.println();

        // Simulate various signal types
        String[] signalTypes = {
            "PropertiesChanged from org.freedesktop.NetworkManager",
            "NameOwnerChanged from org.freedesktop.DBus",
            "DeviceAdded from org.freedesktop.UPower",
            "StateChanged from org.freedesktop.NetworkManager",
            "InterfacesAdded from org.freedesktop.systemd1"
        };

        for (int i = 0; i < Math.min(timeout, signalTypes.length); i++) {
            System.out.println(LocalDateTime.now().format(TIME_FORMAT) + " 📨 " + signalTypes[i]);
            Thread.sleep(2000);
        }

        System.out.println();
        System.out.println("💡 This was a simulation. In a real implementation:");
        System.out.println("   - Signals would be received from actual D-Bus services");
        System.out.println("   - Handler pipeline would process real signal data");
        System.out.println("   - Application logic would react to signal events");
    }

    private static Config parseArgs(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                case "-h":
                    config.showHelp = true;
                    break;
                case "--system-bus":
                    config.dbusAddress = "system";
                    break;
                case "--timeout":
                    if (i + 1 < args.length) {
                        config.timeout = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--interfaces":
                    if (i + 1 < args.length) {
                        config.interfaces = new HashSet<>(Arrays.asList(args[++i].split(",")));
                    }
                    break;
                case "--mode":
                    if (i + 1 < args.length) {
                        config.mode = args[++i];
                    }
                    break;
                case "--verbose":
                    config.verbose = true;
                    break;
            }
        }

        return config;
    }

    private static void showUsage() {
        System.out.println("Usage: SignalHandlingExample [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --help                    Show this help message");
        System.out.println("  --system-bus              Use system bus instead of session bus");
        System.out.println(
                "  --timeout SECONDS         Monitor signals for specified seconds (default: 60)");
        System.out.println(
                "  --interfaces LIST         Comma-separated list of interfaces to monitor");
        System.out.println(
                "  --mode MODE               Execution mode: interactive, demo (default: interactive)");
        System.out.println("  --verbose                 Enable verbose logging");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  SignalHandlingExample --timeout 30");
        System.out.println("  SignalHandlingExample --system-bus --verbose");
        System.out.println(
                "  SignalHandlingExample --interfaces \"org.freedesktop.NetworkManager,org.freedesktop.UPower\"");
        System.out.println("  SignalHandlingExample --mode demo");
    }

    /** Validates D-Bus environment variables for signal handling */
    private static void validateDbusEnvironment(String busType) {
        System.out.println("🔍 Checking D-Bus environment for signal handling...");

        if ("system".equals(busType)) {
            String systemBusAddress = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
            if (systemBusAddress != null) {
                System.out.println("   ✅ DBUS_SYSTEM_BUS_ADDRESS: " + systemBusAddress);
            } else {
                System.out.println("   ℹ️  DBUS_SYSTEM_BUS_ADDRESS: not set (using default)");
            }
        } else {
            String sessionBusAddress = System.getenv("DBUS_SESSION_BUS_ADDRESS");
            if (sessionBusAddress != null) {
                System.out.println("   ✅ DBUS_SESSION_BUS_ADDRESS: " + sessionBusAddress);
            } else {
                System.out.println(
                        "   ⚠️  DBUS_SESSION_BUS_ADDRESS: not set (required for session bus)");
            }
        }

        // Check system properties for SASL
        String userName = System.getProperty("user.name");
        String osName = System.getProperty("os.name");
        System.out.println("   ✅ user.name: " + userName + " (for SASL authentication)");
        System.out.println("   ✅ os.name: " + osName + " (for EXTERNAL mechanism)");

        System.out.println();
    }

    private static class Config {
        boolean showHelp = false;
        String dbusAddress = "session";
        Set<String> interfaces = new HashSet<>();
        int timeout = 60;
        String mode = "interactive";
        boolean verbose = false;
    }

    /** Custom signal handler that demonstrates filtering and processing patterns. */
    private static class SignalHandler extends AbstractInboundHandler {
        private final Set<String> interestedInterfaces;
        private final String mode;
        private long signalCount = 0;

        public SignalHandler(Set<String> interfaces, String mode) {
            this.interestedInterfaces = interfaces;
            this.mode = mode;
        }

        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal signal) {
                // Apply interface filtering if specified
                if (!interestedInterfaces.isEmpty()) {
                    String interfaceName = signal.getInterfaceName().getDelegate();

                    if (!interestedInterfaces.contains(interfaceName)) {
                        // Signal filtered out - don't propagate
                        return;
                    }
                }

                // Process the signal
                processSignal(signal);

                // Don't propagate in demo mode to avoid noise
                if (!mode.equals("demo")) {
                    ctx.propagateInboundMessage(msg);
                }
            } else {
                // Always propagate non-signal messages
                ctx.propagateInboundMessage(msg);
            }
        }

        @Override
        protected Logger getLogger() {
            return null; // provider logger of this class
        }

        private void processSignal(InboundSignal signal) {
            signalCount++;
            String timestamp = LocalDateTime.now().format(TIME_FORMAT);

            String interfaceName = signal.getInterfaceName().getDelegate();
            String memberName = signal.getMember().getDelegate();
            String senderName = signal.getSender().getDelegate();

            System.out.println(timestamp + " 📨 " + interfaceName + "." + memberName);
            System.out.println("     └─ From: " + senderName);
            System.out.println("     └─ Path: " + signal.getObjectPath().getDelegate());

            // Process signal arguments
            List<DBusType> arguments = signal.getPayload();
            if (!arguments.isEmpty()) {
                System.out.println("     └─ Arguments:");
                for (int i = 0; i < arguments.size(); i++) {
                    String argValue = arguments.get(i).toString();
                    if (argValue.length() > 50) {
                        argValue = argValue.substring(0, 47) + "...";
                    }
                    System.out.println("         arg" + i + ": " + argValue);
                }
            }

            // Handle specific signal types
            if ("PropertiesChanged".equals(memberName)) {
                System.out.println("     └─ 🔄 Property change detected");
            } else if ("NameOwnerChanged".equals(memberName)) {
                System.out.println("     └─ 👤 Service ownership changed");
            } else if (memberName.contains("Added") || memberName.contains("Removed")) {
                System.out.println("     └─ ➕➖ Resource change detected");
            }

            System.out.println();

            // Log statistics occasionally
            if (signalCount % 10 == 0) {
                LOGGER.info("Processed {} signals so far", signalCount);
            }
        }
    }
}
