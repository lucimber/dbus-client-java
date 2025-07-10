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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Base class for D-Bus integration tests that provides Docker-based D-Bus test environment.
 * Uses testcontainers-like approach with Docker to provide isolated D-Bus daemon.
 */
public abstract class DBusIntegrationTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(DBusIntegrationTestBase.class);
  
  protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  protected static final String DBUS_CONTAINER_NAME = "dbus-test-container";
  protected static final String DBUS_SOCKET_PATH = "/tmp/dbus-test-socket";
  
  private static Process dockerProcess;
  private static boolean dockerAvailable = false;
  private static String containerSocketPath;

  // Static initializer to check Docker availability early
  static {
    checkDockerAvailability();
  }

  @BeforeAll
  static void setUpDockerDBusEnvironment() {
    if (dockerAvailable) {
      startDockerDBusContainer();
    }
  }

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    LOGGER.info("Starting integration test: {}", testInfo.getDisplayName());
  }

  /**
   * Checks if Docker is available on the system.
   */
  private static void checkDockerAvailability() {
    try {
      LOGGER.info("Checking Docker availability...");
      Process process = new ProcessBuilder("docker", "--version").start();
      int exitCode = process.waitFor();
      
      LOGGER.info("Docker check exit code: {}", exitCode);
      
      if (exitCode == 0) {
        dockerAvailable = true;
        LOGGER.info("Docker found, integration tests will run with containerized D-Bus");
      } else {
        LOGGER.warn("Docker not available (exit code: {}), integration tests will be skipped", exitCode);
      }
    } catch (IOException | InterruptedException e) {
      LOGGER.warn("Could not check for Docker availability: {}", e.getMessage());
      dockerAvailable = false;
    }
  }

  /**
   * Starts a Docker container with D-Bus daemon for testing.
   */
  private static void startDockerDBusContainer() {
    try {
      // Create Dockerfile content for D-Bus container
      Path dockerfilePath = createDBusDockerfile();
      Path contextDir = dockerfilePath.getParent();
      
      LOGGER.info("Building D-Bus test container...");
      
      // Build the container
      Process buildProcess = new ProcessBuilder(
          "docker", "build", 
          "-t", "dbus-test", 
          "-f", dockerfilePath.toString(),
          contextDir.toString()
      ).start();
      
      int buildExitCode = buildProcess.waitFor();
      if (buildExitCode != 0) {
        LOGGER.error("Failed to build D-Bus test container");
        dockerAvailable = false;
        return;
      }
      
      // Start the container with D-Bus daemon
      LOGGER.info("Starting D-Bus test container...");
      
      ProcessBuilder pb = new ProcessBuilder(
          "docker", "run",
          "--rm",
          "--name", DBUS_CONTAINER_NAME,
          "-d",  // Run in background (detached)
          "-p", "12345:12345",  // Expose port 12345
          "-v", "/tmp:/host-tmp",  // Mount host /tmp to access socket
          "dbus-test"
      );
      
      pb.redirectErrorStream(true);
      dockerProcess = pb.start();
      
      // Wait for the docker run command to complete (container will run in background)
      int runExitCode = dockerProcess.waitFor();
      if (runExitCode != 0) {
        LOGGER.error("Failed to start D-Bus test container");
        dockerAvailable = false;
        return;
      }
      
      // Wait for container and D-Bus service to be ready
      LOGGER.info("Waiting for D-Bus service to be ready...");
      if (!waitForDBusService()) {
        LOGGER.error("D-Bus service failed to start within timeout");
        dockerAvailable = false;
        return;
      }
      
      // Get the socket path on host
      containerSocketPath = "/tmp/dbus-test-socket-" + System.currentTimeMillis();
      
      // Set up shutdown hook
      Runtime.getRuntime().addShutdownHook(new Thread(DBusIntegrationTestBase::stopDockerDBusContainer));
      
      LOGGER.info("D-Bus test container started successfully");
      
    } catch (Exception e) {
      LOGGER.error("Failed to start Docker D-Bus container: {}", e.getMessage());
      dockerAvailable = false;
    }
  }

  /**
   * Creates a Dockerfile for the D-Bus test container.
   */
  private static Path createDBusDockerfile() throws IOException {
    String dockerfile = """
        FROM ubuntu:22.04
        
        # Install D-Bus and required tools
        RUN apt-get update && \\
            apt-get install -y dbus dbus-x11 && \\
            rm -rf /var/lib/apt/lists/*
        
        # Create D-Bus configuration for testing
        RUN mkdir -p /etc/dbus-1/session.d
        
        # Create test D-Bus configuration
        COPY dbus-test.conf /etc/dbus-1/session.conf
        
        # Create startup script
        COPY start-dbus.sh /start-dbus.sh
        RUN chmod +x /start-dbus.sh
        
        EXPOSE 12345
        
        CMD ["/start-dbus.sh"]
        """;
    
    Path tempDir = Files.createTempDirectory("dbus-test");
    Path dockerfilePath = tempDir.resolve("Dockerfile");
    Files.write(dockerfilePath, dockerfile.getBytes());
    
    // Create D-Bus config file
    String dbusConfig = """
        <!DOCTYPE busconfig PUBLIC "-//freedesktop//DTD D-BUS Bus Configuration 1.0//EN"
         "http://www.freedesktop.org/standards/dbus/1.0/busconfig.dtd">
        <busconfig>
          <type>session</type>
          <listen>unix:path=/host-tmp/dbus-test-socket</listen>
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
          
          <!-- Explicitly allow connecting and acquiring names -->
          <policy context="mandatory">
            <allow own="*"/>
            <allow send_destination="org.freedesktop.DBus"/>
            <allow receive_sender="org.freedesktop.DBus"/>
          </policy>
        </busconfig>
        """;
    
    Path configPath = tempDir.resolve("dbus-test.conf");
    Files.write(configPath, dbusConfig.getBytes());
    
    // Create startup script
    String startScript = """
        #!/bin/bash
        
        # Create machine ID if it doesn't exist
        if [ ! -f /etc/machine-id ]; then
            dbus-uuidgen > /etc/machine-id
        fi
        
        # Start D-Bus daemon
        exec dbus-daemon --config-file=/etc/dbus-1/session.conf --nofork --print-address
        """;
    
    Path scriptPath = tempDir.resolve("start-dbus.sh");
    Files.write(scriptPath, startScript.getBytes());
    
    // Mark for cleanup
    tempDir.toFile().deleteOnExit();
    dockerfilePath.toFile().deleteOnExit();
    configPath.toFile().deleteOnExit();
    scriptPath.toFile().deleteOnExit();
    
    return dockerfilePath;
  }

  /**
   * Stops the Docker D-Bus container.
   */
  private static void stopDockerDBusContainer() {
    if (dockerProcess != null && dockerProcess.isAlive()) {
      LOGGER.info("Stopping D-Bus test container");
      
      try {
        // Stop the container
        Process stopProcess = new ProcessBuilder("docker", "stop", DBUS_CONTAINER_NAME).start();
        stopProcess.waitFor(10, TimeUnit.SECONDS);
        
        dockerProcess.destroyForcibly();
        dockerProcess.waitFor(5, TimeUnit.SECONDS);
        
      } catch (Exception e) {
        LOGGER.warn("Error stopping Docker container: {}", e.getMessage());
      }
    }
  }

  /**
   * Waits for the D-Bus service in the container to be ready.
   */
  private static boolean waitForDBusService() {
    for (int i = 0; i < 30; i++) {  // Wait up to 30 seconds
      try {
        // Check if container is running
        Process checkProcess = new ProcessBuilder("docker", "exec", DBUS_CONTAINER_NAME, "pgrep", "dbus-daemon").start();
        int exitCode = checkProcess.waitFor();
        
        if (exitCode == 0) {
          // Try to connect to the TCP port
          try (java.net.Socket socket = new java.net.Socket("localhost", 12345)) {
            LOGGER.info("D-Bus service is ready on port 12345");
            return true;
          } catch (java.io.IOException e) {
            // Port not ready yet, continue waiting
          }
        }
        
        Thread.sleep(1000);  // Wait 1 second before next check
      } catch (Exception e) {
        LOGGER.debug("Error checking D-Bus service: {}", e.getMessage());
      }
    }
    
    return false;
  }

  /**
   * Checks if Docker-based D-Bus integration tests should be skipped.
   */
  protected static boolean shouldSkipDBusTests() {
    LOGGER.info("shouldSkipDBusTests() called - dockerAvailable: {}", dockerAvailable);
    return !dockerAvailable;
  }

  /**
   * Gets the D-Bus socket path for testing.
   */
  protected static String getTestDBusSocketPath() {
    return containerSocketPath != null ? containerSocketPath : DBUS_SOCKET_PATH;
  }

  /**
   * Gets the D-Bus address for TCP connection (useful for cross-platform testing).
   */
  protected static String getTestDBusTcpAddress() {
    return "tcp:host=localhost,port=12345";
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
   * Executes a command in the Docker container.
   */
  protected static String execInContainer(String... command) throws IOException, InterruptedException {
    String[] dockerCommand = new String[command.length + 3];
    dockerCommand[0] = "docker";
    dockerCommand[1] = "exec";
    dockerCommand[2] = DBUS_CONTAINER_NAME;
    System.arraycopy(command, 0, dockerCommand, 3, command.length);
    
    Process process = new ProcessBuilder(dockerCommand).start();
    
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
    }
    
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Command failed with exit code: " + exitCode);
    }
    
    return output.toString().trim();
  }

  @FunctionalInterface
  protected interface BooleanSupplier {
    boolean getAsBoolean();
  }
}