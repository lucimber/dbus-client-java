/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Base class for D-Bus integration tests that provides D-Bus test environment. Supports both
 * Testcontainers (external) and in-container (local D-Bus daemon) execution.
 */
public abstract class DBusIntegrationTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBusIntegrationTestBase.class);

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final int DBUS_PORT = 12345;

    // Detect if running inside a container with local D-Bus daemon
    private static final boolean IS_RUNNING_IN_CONTAINER = detectContainerEnvironment();

    // Only initialize container if not running in container mode
    protected static final GenericContainer<?> dbusContainer =
            IS_RUNNING_IN_CONTAINER ? null : createTestContainer();

    @BeforeAll
    static void startTestInfrastructure() {
        if (!IS_RUNNING_IN_CONTAINER && dbusContainer != null) {
            // Start the container manually since we can't use @Testcontainers annotation
            // conditionally
            dbusContainer.start();
            LOGGER.info(
                    "D-Bus container started at {}:{}",
                    dbusContainer.getHost(),
                    dbusContainer.getMappedPort(DBUS_PORT));
        }
    }

    private static GenericContainer<?> createTestContainer() {
        return new GenericContainer<>(
                        new ImageFromDockerfile()
                                .withDockerfileFromBuilder(
                                        builder ->
                                                builder.from("ubuntu:22.04")
                                                        .run(
                                                                "apt-get update && apt-get install -y dbus dbus-x11 netcat-openbsd && rm -rf /var/lib/apt/lists/*")
                                                        .run("mkdir -p /etc/dbus-1/session.d")
                                                        .run("mkdir -p /tmp && chmod 777 /tmp")
                                                        .run(
                                                                "mkdir -p /shared-dbus-cookies && chmod 755 /shared-dbus-cookies")
                                                        .copy(
                                                                "dbus-test.conf",
                                                                "/etc/dbus-1/session.conf")
                                                        .copy("start-dbus.sh", "/start-dbus.sh")
                                                        .run("chmod +x /start-dbus.sh")
                                                        .expose(DBUS_PORT)
                                                        .cmd("/start-dbus.sh")
                                                        .build())
                                .withFileFromString("dbus-test.conf", createDBusConfig())
                                .withFileFromString("start-dbus.sh", createStartScript()))
                .withExposedPorts(DBUS_PORT)
                .waitingFor(
                        Wait.forLogMessage(".*D-Bus daemon ready.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(60)));
    }

    @BeforeEach
    void logTestStart(TestInfo testInfo) {
        LOGGER.info("=== Starting integration test: {} ===", testInfo.getDisplayName());
        LOGGER.info(
                "Test execution mode: {}",
                IS_RUNNING_IN_CONTAINER ? "IN-CONTAINER" : "TESTCONTAINERS");

        // Log environment information for debugging
        if (IS_RUNNING_IN_CONTAINER) {
            LOGGER.info("Container environment detected:");
            LOGGER.info(
                    "  - DBUS_SESSION_BUS_ADDRESS: {}", System.getenv("DBUS_SESSION_BUS_ADDRESS"));
            LOGGER.info(
                    "  - Unix socket exists: {}",
                    new java.io.File("/tmp/dbus-test-socket").exists());
            LOGGER.info("  - Docker env file exists: {}", new java.io.File("/.dockerenv").exists());
        }

        // Log system properties that affect D-Bus
        LOGGER.info("System configuration:");
        LOGGER.info("  - file.encoding: {}", System.getProperty("file.encoding"));
        LOGGER.info("  - user.name: {}", System.getProperty("user.name"));
        LOGGER.info("  - java.version: {}", System.getProperty("java.version"));

        // Force a log flush to ensure this appears in container logs
        System.out.flush();
        System.err.flush();
    }

    /** Detects if we're running inside a container environment with local D-Bus daemon. */
    private static boolean detectContainerEnvironment() {
        // Check for container-specific indicators
        return System.getenv("DBUS_SESSION_BUS_ADDRESS") != null
                || new java.io.File("/tmp/dbus-test-socket").exists()
                || new java.io.File("/.dockerenv").exists();
    }

    /** Gets the D-Bus connection host. */
    protected static String getDBusHost() {
        if (IS_RUNNING_IN_CONTAINER) {
            return "127.0.0.1"; // Local connection within container
        }
        if (dbusContainer == null) {
            throw new IllegalStateException("D-Bus container not initialized");
        }
        return dbusContainer.getHost();
    }

    /** Gets the D-Bus port for connections. */
    protected static int getDBusPort() {
        if (IS_RUNNING_IN_CONTAINER) {
            return DBUS_PORT; // Direct port within container
        }
        if (dbusContainer == null) {
            throw new IllegalStateException("D-Bus container not initialized");
        }
        return dbusContainer.getMappedPort(DBUS_PORT);
    }

    /** Gets the D-Bus TCP address for connections. */
    protected static String getDBusTcpAddress() {
        return "tcp:host=" + getDBusHost() + ",port=" + getDBusPort();
    }

    /** Gets the D-Bus Unix socket address for connections. */
    protected static String getDBusUnixAddress() {
        return "unix:path=/tmp/dbus-test-socket";
    }

    /**
     * Determines if Unix socket should be preferred over TCP. In container mode, Unix socket is
     * always preferred for better authentication.
     */
    protected static boolean shouldPreferUnixSocket() {
        if (IS_RUNNING_IN_CONTAINER) {
            return true; // Always prefer Unix socket when running in container
        }
        // For Testcontainers, use TCP due to cross-platform considerations
        return false;
    }

    /** Returns true if we're running in container mode with local D-Bus daemon. */
    protected static boolean isRunningInContainer() {
        return IS_RUNNING_IN_CONTAINER;
    }

    /** Creates the D-Bus configuration file content. */
    private static String createDBusConfig() {
        return """
    <!DOCTYPE busconfig PUBLIC "-//freedesktop//DTD D-BUS Bus Configuration 1.0//EN"
         "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
    <busconfig>
          <type>session</type>
          <listen>unix:path=/tmp/dbus-test-socket</listen>
          <listen>tcp:host=0.0.0.0,port=12345</listen>

          <!-- For testing purposes, use DBUS_COOKIE_SHA1 which works better for TCP -->
          <auth>DBUS_COOKIE_SHA1</auth>
          <auth>EXTERNAL</auth>

          <standard_session_servicedirs />

          <!-- Service definitions for basic D-Bus functionality -->
          <servicedir>/usr/share/dbus-1/services</servicedir>

          <!-- Policy to allow everything for testing -->
          <policy context="default">
      <allow send_destination="*"/>
      <allow send_interface="*"/>
      <allow receive_sender="*"/>
      <allow receive_interface="*"/>
      <allow own="*"/>
      <allow user="*"/>
      <allow eavesdrop="true"/>
          </policy>
    </busconfig>
    """;
    }

    /** Creates the startup script for the D-Bus daemon. */
    private static String createStartScript() {
        return """
    #!/bin/bash

    # Create machine ID if it doesn't exist
    if [ ! -f /etc/machine-id ]; then
      dbus-uuidgen > /etc/machine-id
    fi

    # Create D-Bus keyring directory for DBUS_COOKIE_SHA1 auth
    mkdir -p ~/.dbus-keyrings
    chmod 700 ~/.dbus-keyrings

    # Create a simple cookie for testing that can be read by clients
    # Generate a cookie in the shared directory for cross-platform access
    mkdir -p /shared-dbus-cookies
    chmod 755 /shared-dbus-cookies

    # Create a predictable cookie for integration testing
    COOKIE_ID="1"
    COOKIE_TIME=$(date +%s)
    COOKIE_VALUE="$(openssl rand -hex 32 2>/dev/null || dd if=/dev/urandom bs=32 count=1 2>/dev/null | xxd -p | tr -d '\\n')"

    echo "$COOKIE_ID $COOKIE_TIME $COOKIE_VALUE" > ~/.dbus-keyrings/org_freedesktop_general
    chmod 600 ~/.dbus-keyrings/org_freedesktop_general

    # Also copy to shared location (though this won't work cross-platform without volume mounting)
    cp ~/.dbus-keyrings/org_freedesktop_general /shared-dbus-cookies/ 2>/dev/null || true

    # Start D-Bus daemon in background and wait for it to be ready
    echo "Starting D-Bus daemon..."
    echo "D-Bus daemon configuration:"
    echo "- Auth mechanisms: DBUS_COOKIE_SHA1 (preferred), EXTERNAL"
    echo "- Listening on: TCP port 12345, Unix socket /tmp/dbus-test-socket"
    echo "- Cookie created: $COOKIE_VALUE"

    dbus-daemon --config-file=/etc/dbus-1/session.conf --nofork --print-address &
    DBUS_PID=$!

    # Wait for D-Bus to be ready by checking if it's listening on the port
    for i in {1..30}; do
      if netcat -z 0.0.0.0 12345; then
        echo "D-Bus daemon ready"
        echo "Authentication cookie file:"
        ls -la ~/.dbus-keyrings/
        break
      fi
      sleep 1
    done

    # Keep the container alive by waiting for the D-Bus daemon
    wait $DBUS_PID
    """;
    }

    /**
     * Checks if D-Bus integration tests should be skipped. In container mode, checks for local
     * D-Bus availability. With Testcontainers, this is automatically handled by the framework.
     */
    protected static boolean shouldSkipDBusTests() {
        if (IS_RUNNING_IN_CONTAINER) {
            // Check if D-Bus daemon is running locally
            try {
                // Check if Unix socket exists or TCP port is listening
                return !new java.io.File("/tmp/dbus-test-socket").exists()
                        && !isPortListening("127.0.0.1", DBUS_PORT);
            } catch (Exception e) {
                LOGGER.warn("Error checking D-Bus availability: {}", e.getMessage());
                return true;
            }
        }
        // Testcontainers automatically handles Docker availability
        return false;
    }

    /** Checks if a port is listening on the given host. */
    private static boolean isPortListening(String host, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Sleeps for the specified duration, handling interruption gracefully. */
    protected static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }

    /** Waits for a condition to become true within the specified timeout. */
    protected static boolean waitFor(
            Duration timeout, Duration checkInterval, BooleanSupplier condition) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return true;
            }
            sleep(checkInterval);
        }

        return false;
    }

    /** Executes a command in the D-Bus container using Testcontainers. */
    protected static String execInContainer(String... command)
            throws IOException, InterruptedException {
        if (IS_RUNNING_IN_CONTAINER) {
            throw new UnsupportedOperationException(
                    "execInContainer not supported when running inside container");
        }
        if (dbusContainer == null) {
            throw new IllegalStateException("D-Bus container not initialized");
        }
        try {
            var result = dbusContainer.execInContainer(command);
            if (result.getExitCode() != 0) {
                throw new RuntimeException(
                        "Command failed with exit code: "
                                + result.getExitCode()
                                + ", stderr: "
                                + result.getStderr());
            }
            return result.getStdout();
        } catch (UnsupportedOperationException e) {
            throw new IOException("Container exec not supported", e);
        }
    }

    @FunctionalInterface
    protected interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
