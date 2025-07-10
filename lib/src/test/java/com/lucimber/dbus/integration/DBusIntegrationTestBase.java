/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Base class for D-Bus integration tests that provides Docker-based D-Bus test environment.
 * Uses Testcontainers framework for robust container lifecycle management.
 */
@Testcontainers
public abstract class DBusIntegrationTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(DBusIntegrationTestBase.class);
  
  protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  private static final int DBUS_PORT = 12345;

  @Container
  protected static final GenericContainer<?> dbusContainer = new GenericContainer<>(
      new ImageFromDockerfile()
          .withDockerfileFromBuilder(builder -> 
              builder
                  .from("ubuntu:22.04")
                  .run("apt-get update && apt-get install -y dbus dbus-x11 netcat-openbsd && rm -rf /var/lib/apt/lists/*")
                  .run("mkdir -p /etc/dbus-1/session.d")
                  .copy("dbus-test.conf", "/etc/dbus-1/session.conf")
                  .copy("start-dbus.sh", "/start-dbus.sh")
                  .run("chmod +x /start-dbus.sh")
                  .expose(DBUS_PORT)
                  .cmd("/start-dbus.sh")
                  .build())
          .withFileFromString("dbus-test.conf", createDBusConfig())
          .withFileFromString("start-dbus.sh", createStartScript())
  )
  .withExposedPorts(DBUS_PORT)
  .waitingFor(Wait.forLogMessage(".*D-Bus daemon ready.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    LOGGER.info("Starting integration test: {}", testInfo.getDisplayName());
  }

  /**
   * Gets the D-Bus connection host. With Testcontainers, this is always localhost.
   */
  protected static String getDBusHost() {
    return dbusContainer.getHost();
  }

  /**
   * Gets the mapped D-Bus port from the container.
   */
  protected static int getDBusPort() {
    return dbusContainer.getMappedPort(DBUS_PORT);
  }

  /**
   * Gets the D-Bus TCP address for connections.
   */
  protected static String getDBusTcpAddress() {
    return "tcp:host=" + getDBusHost() + ",port=" + getDBusPort();
  }

  /**
   * Creates the D-Bus configuration file content.
   */
  private static String createDBusConfig() {
    return """
        <!DOCTYPE busconfig PUBLIC "-//freedesktop//DTD D-BUS Bus Configuration 1.0//EN"
         "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
        <busconfig>
          <type>session</type>
          <listen>tcp:host=0.0.0.0,port=12345</listen>
          <auth>ANONYMOUS</auth>
          <allow_anonymous/>
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

  /**
   * Creates the startup script for the D-Bus daemon.
   */
  private static String createStartScript() {
    return """
        #!/bin/bash
        
        # Create machine ID if it doesn't exist
        if [ ! -f /etc/machine-id ]; then
            dbus-uuidgen > /etc/machine-id
        fi
        
        # Start D-Bus daemon in background and wait for it to be ready
        echo "Starting D-Bus daemon..."
        dbus-daemon --config-file=/etc/dbus-1/session.conf --nofork --print-address &
        DBUS_PID=$!
        
        # Wait for D-Bus to be ready by checking if it's listening on the port
        for i in {1..30}; do
            if netcat -z 0.0.0.0 12345; then
                echo "D-Bus daemon ready"
                break
            fi
            sleep 1
        done
        
        # Keep the container alive by waiting for the D-Bus daemon
        wait $DBUS_PID
        """;
  }

  /**
   * Checks if Docker-based D-Bus integration tests should be skipped.
   * With Testcontainers, this is automatically handled by the framework.
   */
  protected static boolean shouldSkipDBusTests() {
    // Testcontainers automatically handles Docker availability
    // Tests will be skipped automatically if Docker is not available
    return false;
  }

  /**
   * Sleeps for the specified duration, handling interruption gracefully.
   */
  protected static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Test interrupted", e);
    }
  }

  /**
   * Waits for a condition to become true within the specified timeout.
   */
  protected static boolean waitFor(Duration timeout, Duration checkInterval, BooleanSupplier condition) {
    long endTime = System.currentTimeMillis() + timeout.toMillis();
    
    while (System.currentTimeMillis() < endTime) {
      if (condition.getAsBoolean()) {
        return true;
      }
      sleep(checkInterval);
    }
    
    return false;
  }

  /**
   * Executes a command in the D-Bus container using Testcontainers.
   */
  protected static String execInContainer(String... command) throws IOException, InterruptedException {
    try {
      var result = dbusContainer.execInContainer(command);
      if (result.getExitCode() != 0) {
        throw new RuntimeException("Command failed with exit code: " + result.getExitCode() + 
                                 ", stderr: " + result.getStderr());
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