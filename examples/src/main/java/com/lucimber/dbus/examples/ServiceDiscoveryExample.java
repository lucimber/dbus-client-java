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
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive example demonstrating D-Bus service discovery and introspection.
 * <p>
 * This example shows how to:
 * - Discover available services on the D-Bus
 * - Introspect service interfaces and methods
 * - Query service properties
 * - Handle service lifecycle events
 */
public class ServiceDiscoveryExample {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscoveryExample.class);

  private static final String DBUS_SERVICE = "org.freedesktop.DBus";
  private static final String DBUS_PATH = "/org/freedesktop/DBus";
  private static final String DBUS_INTERFACE = "org.freedesktop.DBus";
  private static final String INTROSPECTABLE_INTERFACE = "org.freedesktop.DBus.Introspectable";

  public static void main(String[] args) throws Exception {
    // Parse command line arguments
    Config config = parseArgs(args);

    if (config.showHelp) {
      showUsage();
      return;
    }

    System.out.println("=== D-Bus Service Discovery Example ===");
    System.out.println();
    System.out.println("Configuration:");
    System.out.println("  D-Bus Address: " + config.dbusAddress);
    System.out.println("  Filter: " + (config.serviceFilter.isEmpty() ? "ALL" : config.serviceFilter));
    System.out.println("  Timeout: " + config.timeout + "s");
    System.out.println("  Mode: " + config.mode);
    System.out.println();

    // Validate environment variables for D-Bus connection
    validateDbusEnvironment(config.dbusAddress);

    // Create connection configuration
    ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.builder()
            .withConnectTimeout(Duration.ofSeconds(30))
            .withAutoReconnectEnabled(true);

    try (Connection connection = config.dbusAddress.contains("system")
            ? NettyConnection.newSystemBusConnection(connectionConfigBuilder.build())
            : NettyConnection.newSessionBusConnection(connectionConfigBuilder.build())) {
      // Connect to D-Bus
      System.out.println("📡 Connecting to D-Bus...");
      connection.connect().toCompletableFuture().get(30, TimeUnit.SECONDS);
      System.out.println("✅ Connected to D-Bus");
      System.out.println();

      if (config.mode.equals("demo")) {
        runDemo();
      } else {
        // Discover services
        discoverServices(connection, config);
      }

    } finally {
      System.out.println("✅ Service discovery completed");
    }
  }

  private static void discoverServices(Connection connection, Config config) throws Exception {
    System.out.println("🔍 Discovering D-Bus services...");

    // Get list of available services
    List<String> services = listServices(connection);

    if (services.isEmpty()) {
      System.out.println("❌ No services found");
      return;
    }

    // Filter services if specified
    if (!config.serviceFilter.isEmpty()) {
      services = services.stream()
              .filter(service -> service.contains(config.serviceFilter))
              .toList();
    }

    System.out.println("📋 Found " + services.size() + " services:");
    System.out.println();

    // Display services
    for (String service : services) {
      System.out.println("🔧 Service: " + service);

      if (config.verbose) {
        // Get service owner
        String owner = getServiceOwner(connection, service);
        System.out.println("   └─ Owner: " + owner);

        // Try to introspect the service
        if (config.introspect) {
          introspectService(connection, service);
        }
      }

      System.out.println();
    }

    // Show statistics
    System.out.println("📊 Discovery Summary:");
    System.out.println("   Total services: " + services.size());
    System.out.println("   Well-known names: " + services.stream()
            .mapToLong(s -> s.startsWith(":") ? 0 : 1)
            .sum());
    System.out.println("   Unique names: " + services.stream()
            .mapToLong(s -> s.startsWith(":") ? 1 : 0)
            .sum());
  }

  private static List<String> listServices(Connection connection) throws Exception {
    System.out.println("   Querying service list...");

    // Create method call to list names
    OutboundMethodCall listNamesCall = OutboundMethodCall.Builder
            .create()
            .withPath(DBusObjectPath.valueOf(DBUS_PATH))
            .withInterface(DBusString.valueOf(DBUS_INTERFACE))
            .withMember(DBusString.valueOf("ListNames"))
            .withDestination(DBusString.valueOf(DBUS_SERVICE))
            .withReplyExpected(true)
            .build();

    // Send request and wait for response
    CompletionStage<InboundMessage> responseFuture = connection.sendRequest(listNamesCall);
    InboundMessage response = responseFuture.toCompletableFuture().get(30, TimeUnit.SECONDS);

    if (response instanceof InboundMethodReturn methodReturn) {
      List<DBusType> payload = methodReturn.getPayload();

      if (!payload.isEmpty() && payload.get(0) instanceof DBusArray) {
        @SuppressWarnings("unchecked")
        DBusArray<DBusString> serviceArray = (DBusArray<DBusString>) payload.get(0);

        List<String> services = new ArrayList<>();
        for (DBusString serviceString : serviceArray) {
          services.add(serviceString.getDelegate());
        }

        Collections.sort(services);
        return services;
      }
    } else if (response instanceof InboundError error) {
      System.err.println("❌ Error listing services: " + error.getErrorName());
    }

    return Collections.emptyList();
  }

  private static String getServiceOwner(Connection connection, String serviceName) {
    try {
      // Skip unique names (they own themselves)
      if (serviceName.startsWith(":")) {
        return serviceName;
      }

      // Create method call to get name owner
      OutboundMethodCall getOwnerCall = OutboundMethodCall.Builder
              .create()
              .withPath(DBusObjectPath.valueOf(DBUS_PATH))
              .withInterface(DBusString.valueOf(DBUS_INTERFACE))
              .withMember(DBusString.valueOf("GetNameOwner"))
              .withDestination(DBusString.valueOf(DBUS_SERVICE))
              .withBody(DBusSignature.valueOf("s"), List.of(DBusString.valueOf(serviceName)))
              .withReplyExpected(true)
              .build();

      // Send request and wait for response
      CompletionStage<InboundMessage> responseFuture = connection.sendRequest(getOwnerCall);
      InboundMessage response = responseFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);

      if (response instanceof InboundMethodReturn methodReturn) {
        List<DBusType> payload = methodReturn.getPayload();

        if (!payload.isEmpty() && payload.get(0) instanceof DBusString) {
          return ((DBusString) payload.get(0)).getDelegate();
        }
      }
    } catch (Exception e) {
      LOGGER.debug("Failed to get owner for service {}: {}", serviceName, e.getMessage());
    }

    return "unknown";
  }

  private static void introspectService(Connection connection, String serviceName) {
    try {
      // Create method call to introspect
      OutboundMethodCall introspectCall = OutboundMethodCall.Builder
              .create()
              .withPath(DBusObjectPath.valueOf("/"))
              .withInterface(DBusString.valueOf(INTROSPECTABLE_INTERFACE))
              .withMember(DBusString.valueOf("Introspect"))
              .withDestination(DBusString.valueOf(serviceName))
              .withReplyExpected(true)
              .build();

      // Send request and wait for response
      CompletionStage<InboundMessage> responseFuture = connection.sendRequest(introspectCall);
      InboundMessage response = responseFuture.toCompletableFuture().get(10, TimeUnit.SECONDS);

      if (response instanceof InboundMethodReturn methodReturn) {
        List<DBusType> payload = methodReturn.getPayload();

        if (!payload.isEmpty() && payload.get(0) instanceof DBusString) {
          String introspectionXml = ((DBusString) payload.get(0)).getDelegate();
          System.out.println("   └─ Introspection available (" + introspectionXml.length() + " chars)");

          // Parse basic info from XML
          if (introspectionXml.contains("<interface")) {
            long interfaceCount = introspectionXml.split("<interface").length - 1;
            System.out.println("   └─ Interfaces: " + interfaceCount);
          }
          if (introspectionXml.contains("<method")) {
            long methodCount = introspectionXml.split("<method").length - 1;
            System.out.println("   └─ Methods: " + methodCount);
          }
          if (introspectionXml.contains("<signal")) {
            long signalCount = introspectionXml.split("<signal").length - 1;
            System.out.println("   └─ Signals: " + signalCount);
          }
        }
      } else if (response instanceof InboundError error) {
        System.out.println("   └─ Introspection failed: " + error.getErrorName());
      }
    } catch (Exception e) {
      System.out.println("   └─ Introspection error: " + e.getMessage());
    }
  }

  private static void runDemo() throws InterruptedException {
    System.out.println("🎬 Running in demo mode...");
    System.out.println("📋 Simulating service discovery:");
    System.out.println();

    // Simulate service discovery
    String[] services = {
            "org.freedesktop.DBus",
            "org.freedesktop.NetworkManager",
            "org.freedesktop.UPower",
            "org.freedesktop.systemd1",
            "org.freedesktop.login1",
            ":1.123",
            ":1.124",
            ":1.125"
    };

    System.out.println("🔍 Discovered " + services.length + " services:");
    System.out.println();

    for (String service : services) {
      System.out.println("🔧 Service: " + service);
      if (service.startsWith(":")) {
        System.out.println("   └─ Owner: " + service + " (unique name)");
      } else {
        System.out.println("   └─ Owner: :1." + (100 + Math.abs(service.hashCode()) % 100));
        System.out.println("   └─ Introspection available (1234 chars)");
        System.out.println("   └─ Interfaces: 3");
        System.out.println("   └─ Methods: 12");
        System.out.println("   └─ Signals: 5");
      }
      System.out.println();
      Thread.sleep(500);
    }

    System.out.println("📊 Discovery Summary:");
    System.out.println("   Total services: " + services.length);
    System.out.println("   Well-known names: 5");
    System.out.println("   Unique names: 3");
    System.out.println();

    System.out.println("💡 This was a simulation. In a real implementation:");
    System.out.println("   - Services would be discovered from actual D-Bus daemon");
    System.out.println("   - Introspection would return real XML data");
    System.out.println("   - Service owners would be actual connection names");
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
        case "--filter":
          if (i + 1 < args.length) {
            config.serviceFilter = args[++i];
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
        case "--introspect":
          config.introspect = true;
          break;
      }
    }

    return config;
  }

  private static void showUsage() {
    System.out.println("Usage: ServiceDiscoveryExample [OPTIONS]");
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --help                    Show this help message");
    System.out.println("  --system-bus              Use system bus instead of session bus");
    System.out.println("  --timeout SECONDS         Discovery timeout in seconds (default: 30)");
    System.out.println("  --filter PATTERN          Filter services by name pattern");
    System.out.println("  --mode MODE               Execution mode: interactive, demo (default: interactive)");
    System.out.println("  --verbose                 Show detailed service information");
    System.out.println("  --introspect              Attempt to introspect discovered services");
    System.out.println();
    System.out.println("Examples:");
    System.out.println("  ServiceDiscoveryExample --verbose");
    System.out.println("  ServiceDiscoveryExample --system-bus --filter freedesktop");
    System.out.println("  ServiceDiscoveryExample --introspect --timeout 60");
    System.out.println("  ServiceDiscoveryExample --mode demo");
  }

  /**
   * Validates D-Bus environment variables for service discovery
   */
  private static void validateDbusEnvironment(String busType) {
    System.out.println("🔍 Checking D-Bus environment for service discovery...");
    
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
        System.out.println("   ⚠️  DBUS_SESSION_BUS_ADDRESS: not set (required for session bus)");
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
    String serviceFilter = "";
    int timeout = 30;
    String mode = "interactive";
    boolean verbose = false;
    boolean introspect = false;
  }
}