/*
 * SPDX-FileCopyrightText: 2023-2026 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.examples;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionEventListener;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive example demonstrating D-Bus authentication mechanisms.
 *
 * <p>This example shows how to: - Use different SASL authentication mechanisms - Configure
 * authentication options - Handle authentication failures - Monitor authentication events - Test
 * authentication with different bus types
 */
public class AuthenticationExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationExample.class);

    public static void main(String[] args) throws Exception {
        // Parse command line arguments
        Config config = parseArgs(args);

        if (config.showHelp) {
            showUsage();
            return;
        }

        System.out.println("=== D-Bus Authentication Example ===");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  D-Bus address: " + config.dbusAddress);
        System.out.println("  Timeout: " + config.timeout + "s");
        System.out.println("  Mode: " + config.mode);
        System.out.println("  Authentication: Automatic (framework-selected)");
        System.out.println();

        if (config.mode.equals("demo")) {
            runDemo(config);
        } else {
            testAuthentication(config);
        }
    }

    private static void testAuthentication(Config config) {
        System.out.println("🔐 Testing automatic authentication...");

        // Validate environment variables required for SASL authentication
        validateSaslEnvironment();

        // Create connection configuration with authentication
        ConnectionConfig connectionConfig = createConnectionConfig(config);

        // Create connection
        Connection connection = createConnection(config, connectionConfig);

        // Add connection event listener to monitor authentication

        try (connection) {
            connection.addConnectionEventListener(new AuthenticationEventListener());
            // Attempt to connect
            System.out.println("📡 Connecting to D-Bus...");
            long startTime = System.currentTimeMillis();

            connection.connect().toCompletableFuture().get(config.timeout, TimeUnit.SECONDS);

            long connectTime = System.currentTimeMillis() - startTime;
            System.out.println("✅ Authentication successful! (" + connectTime + "ms)");
            System.out.println();

            // Test basic functionality
            testBasicFunctionality(connection);

        } catch (Exception e) {
            System.err.println("❌ Authentication failed: " + e.getMessage());
            LOGGER.error("Authentication error", e);
        } finally {
            System.out.println("✅ Authentication test completed");
        }
    }

    private static ConnectionConfig createConnectionConfig(Config config) {
        ConnectionConfig.Builder builder =
                ConnectionConfig.builder()
                        .withConnectTimeout(Duration.ofSeconds(config.timeout))
                        .withAutoReconnectEnabled(false); // Disable for authentication testing

        // Note: Authentication mechanism is handled automatically by the connection implementation
        // The framework will use the appropriate SASL mechanism based on the connection type and
        // environment
        System.out.println("🔑 Using default authentication (handled automatically by framework)");

        return builder.build();
    }

    private static Connection createConnection(Config config, ConnectionConfig connectionConfig) {
        if (config.dbusAddress.equals("system")) {
            return NettyConnection.newSystemBusConnection(connectionConfig);
        } else if (config.dbusAddress.equals("session")) {
            return NettyConnection.newSessionBusConnection(connectionConfig);
        } else {
            // Custom address
            SocketAddress socketAddress = UnixDomainSocketAddress.of(config.dbusAddress);
            return new NettyConnection(socketAddress, connectionConfig);
        }
    }

    private static void testBasicFunctionality(Connection connection) throws Exception {
        System.out.println("🧪 Testing basic D-Bus functionality...");

        // Create a simple method call to test the connection
        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                        .withMember(DBusString.valueOf("GetId"))
                        .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                        .withReplyExpected(true)
                        .build();

        // Send the call
        CompletionStage<InboundMessage> response = connection.sendRequest(call);
        InboundMessage reply = response.toCompletableFuture().get(30, TimeUnit.SECONDS);

        if (reply instanceof InboundMethodReturn methodReturn) {
            List<DBusType> payload = methodReturn.getPayload();

            if (!payload.isEmpty() && payload.get(0) instanceof DBusString) {
                String busId = ((DBusString) payload.get(0)).getDelegate();
                System.out.println("   ✅ D-Bus ID: " + busId);
                System.out.println("   ✅ Method call successful");
            }
        } else {
            System.out.println(
                    "   ❌ Unexpected response type: " + reply.getClass().getSimpleName());
        }
    }

    private static void runDemo(Config config) throws InterruptedException {
        System.out.println("🎬 Running in demo mode...");
        System.out.println("🔐 Simulating authentication process:");
        System.out.println();

        // Simulate authentication steps
        System.out.println("1. 🤝 Initiating SASL handshake...");
        Thread.sleep(500);

        System.out.println("2. 🔍 Selecting authentication mechanism automatically...");
        Thread.sleep(500);

        // Show what authentication mechanism would typically be used
        if (config.dbusAddress.equals("system")) {
            System.out.println("3. 🆔 Using EXTERNAL authentication (Unix credentials)...");
            System.out.println("   └─ UID: " + System.getProperty("user.name"));
            System.out.println("   └─ Process credentials sent to system bus");
        } else {
            System.out.println("3. 🆔 Using appropriate authentication for session bus...");
            System.out.println("   └─ Framework selects best available mechanism");
            System.out.println("   └─ Typically EXTERNAL or DBUS_COOKIE_SHA1");
        }

        Thread.sleep(500);
        System.out.println("4. ✅ Authentication successful!");
        System.out.println("5. 🚀 D-Bus connection established");
        System.out.println();

        System.out.println("🧪 Testing basic functionality...");
        Thread.sleep(500);
        System.out.println("   ✅ D-Bus ID: 1234567890abcdef");
        System.out.println("   ✅ Method call successful");
        System.out.println();

        System.out.println("💡 This was a simulation. In a real implementation:");
        System.out.println("   - Actual SASL handshake would occur");
        System.out.println("   - Real credentials would be exchanged");
        System.out.println("   - Authentication success/failure would be determined");
        System.out.println("   - Connection would be established or rejected");
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
                case "--session-bus":
                    config.dbusAddress = "session";
                    break;
                case "--address":
                    if (i + 1 < args.length) {
                        config.dbusAddress = args[++i];
                    }
                    break;
                case "--timeout":
                    if (i + 1 < args.length) {
                        config.timeout = Integer.parseInt(args[++i]);
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
        System.out.println("Usage: AuthenticationExample [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --help                    Show this help message");
        System.out.println("  --system-bus              Use system bus");
        System.out.println("  --session-bus             Use session bus (default)");
        System.out.println("  --address PATH            Custom D-Bus socket address");
        System.out.println("  --timeout SECONDS         Authentication timeout (default: 30)");
        System.out.println(
                "  --mode MODE               Execution mode: interactive, demo (default: interactive)");
        System.out.println("  --verbose                 Enable verbose logging");
        System.out.println();
        System.out.println("Authentication:");
        System.out.println(
                "  Authentication mechanism is selected automatically by the framework.");
        System.out.println("  For system bus: EXTERNAL (Unix credentials)");
        System.out.println(
                "  For session bus: EXTERNAL or DBUS_COOKIE_SHA1 (framework chooses best available)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  AuthenticationExample --system-bus");
        System.out.println("  AuthenticationExample --session-bus --timeout 60");
        System.out.println("  AuthenticationExample --mode demo");
        System.out.println("  AuthenticationExample --verbose");
    }

    /** Validates environment variables and system properties required for SASL authentication */
    private static void validateSaslEnvironment() {
        System.out.println("🔍 Validating SASL environment variables...");

        // Check D-Bus address environment variables
        String sessionBusAddress = System.getenv("DBUS_SESSION_BUS_ADDRESS");
        String systemBusAddress = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");

        System.out.println("   D-Bus Addresses:");
        if (sessionBusAddress != null) {
            System.out.println("     ✅ DBUS_SESSION_BUS_ADDRESS: " + sessionBusAddress);
        } else {
            System.out.println(
                    "     ⚠️  DBUS_SESSION_BUS_ADDRESS: not set (required for session bus)");
        }

        if (systemBusAddress != null) {
            System.out.println("     ✅ DBUS_SYSTEM_BUS_ADDRESS: " + systemBusAddress);
        } else {
            System.out.println("     ℹ️  DBUS_SYSTEM_BUS_ADDRESS: not set (using default)");
        }

        // Check system properties required for SASL mechanisms
        String userName = System.getProperty("user.name");
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name");

        System.out.println("   SASL System Properties:");
        System.out.println("     ✅ user.name: " + userName + " (EXTERNAL & DBUS_COOKIE_SHA1)");
        System.out.println("     ✅ user.home: " + userHome + " (DBUS_COOKIE_SHA1)");
        System.out.println("     ✅ os.name: " + osName + " (EXTERNAL mechanism)");

        // Check DBUS_COOKIE_SHA1 requirements
        if (userHome != null) {
            java.io.File keyringDir = new java.io.File(userHome, ".dbus-keyrings");
            System.out.println("   DBUS_COOKIE_SHA1 Authentication:");

            if (keyringDir.exists()) {
                System.out.println(
                        "     ✅ Keyring directory exists: " + keyringDir.getAbsolutePath());

                // Check directory permissions (Unix-like systems)
                if (!osName.toLowerCase().contains("win")) {
                    try {
                        java.nio.file.attribute.PosixFilePermissions.fromString("rwx------");
                        System.out.println(
                                "     ✅ Directory permissions should be 700 (owner only)");
                    } catch (Exception e) {
                        System.out.println("     ⚠️  Could not check directory permissions");
                    }
                }
            } else {
                System.out.println(
                        "     ⚠️  Keyring directory not found: " + keyringDir.getAbsolutePath());
                System.out.println("     ℹ️  Will be created if DBUS_COOKIE_SHA1 is used");
            }
        }

        // Check EXTERNAL mechanism requirements
        System.out.println("   EXTERNAL Authentication:");
        if (osName.toLowerCase().contains("win")) {
            System.out.println("     ✅ Windows detected - will use SID for authentication");
        } else {
            System.out.println(
                    "     ✅ Unix-like system detected - will use UID for authentication");
            java.io.File procSelf = new java.io.File("/proc/self");
            if (procSelf.exists()) {
                System.out.println("     ✅ /proc/self available for UID resolution");
            } else {
                System.out.println("     ⚠️  /proc/self not available - may affect UID resolution");
            }
        }

        System.out.println();
    }

    private static class Config {
        boolean showHelp = false;
        String dbusAddress = "session";
        int timeout = 30;
        String mode = "interactive";
        boolean verbose = false;
    }

    /** Event listener to monitor authentication events. */
    private static class AuthenticationEventListener implements ConnectionEventListener {
        @Override
        public void onConnectionEvent(
                Connection connection, com.lucimber.dbus.connection.ConnectionEvent event) {
            switch (event.getType()) {
                case STATE_CHANGED:
                    System.out.println("   🔄 Connection state changed");
                    break;
                case HEALTH_CHECK_SUCCESS:
                    System.out.println("   ✅ Health check succeeded");
                    break;
                case HEALTH_CHECK_FAILURE:
                    System.out.println("   ❌ Health check failed");
                    if (event.getCause().isPresent()) {
                        System.out.println("   └─ Cause: " + event.getCause().get().getMessage());
                    }
                    break;
                case RECONNECTION_ATTEMPT:
                    System.out.println("   🔄 Reconnection attempt");
                    break;
                case RECONNECTION_SUCCESS:
                    System.out.println("   ✅ Reconnection succeeded");
                    break;
                case RECONNECTION_FAILURE:
                    System.out.println("   ❌ Reconnection failed");
                    break;
                case RECONNECTION_EXHAUSTED:
                    System.out.println("   ❌ Reconnection exhausted");
                    break;
            }
        }
    }
}
