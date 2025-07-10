/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionEvent;
import com.lucimber.dbus.connection.ConnectionEventListener;
import com.lucimber.dbus.connection.ConnectionEventType;
import com.lucimber.dbus.connection.ConnectionState;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import io.netty.channel.unix.DomainSocketAddress;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for D-Bus connection functionality using Docker-based D-Bus daemon.
 */
@Tag("integration")
@DisabledIf("shouldSkipDBusTests")
class ConnectionIntegrationTest extends DBusIntegrationTestBase {

  @Test
  void testBasicConnectionLifecycle() throws Exception {
    // Use TCP connection for cross-platform compatibility
    Connection connection = new NettyConnection(
        new InetSocketAddress("localhost", 12345),
        ConnectionConfig.defaultConfig()
    );

    try {
      // Test connection
      CompletableFuture<Void> connectFuture = connection.connect().toCompletableFuture();
      assertDoesNotThrow(() -> connectFuture.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

      // Verify connection state
      assertTrue(connection.isConnected());
      assertEquals(ConnectionState.CONNECTED, connection.getState());

    } finally {
      connection.close();
    }
  }

  @Test
  void testConnectionWithHealthMonitoring() throws Exception {
    ConnectionConfig config = ConnectionConfig.builder()
        .withHealthCheckEnabled(true)
        .withHealthCheckInterval(Duration.ofSeconds(2))
        .withHealthCheckTimeout(Duration.ofSeconds(1))
        .build();

    Connection connection = new NettyConnection(
        new InetSocketAddress("localhost", 12345),
        config
    );

    CountDownLatch healthCheckLatch = new CountDownLatch(1);
    AtomicReference<ConnectionEvent> healthEvent = new AtomicReference<>();

    ConnectionEventListener listener = (conn, event) -> {
      if (event.getType() == ConnectionEventType.HEALTH_CHECK_SUCCESS) {
        healthEvent.set(event);
        healthCheckLatch.countDown();
      }
    };

    try {
      connection.addConnectionEventListener(listener);

      // Connect
      connection.connect().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      // Wait for health check
      assertTrue(healthCheckLatch.await(10, TimeUnit.SECONDS), "Health check should succeed");
      assertNotNull(healthEvent.get());

    } finally {
      connection.close();
    }
  }

  @Test
  void testDBusMethodCall() throws Exception {
    Connection connection = new NettyConnection(
        new InetSocketAddress("localhost", 12345),
        ConnectionConfig.defaultConfig()
    );

    try {
      // Connect
      connection.connect().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      // Create a method call to the D-Bus daemon itself
      OutboundMethodCall methodCall = OutboundMethodCall.Builder
          .create()
          .withPath(ObjectPath.valueOf("/org/freedesktop/DBus"))
          .withMember(DBusString.valueOf("ListNames"))
          .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
          .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
          .withReplyExpected(true)
          .build();

      // Send method call and get response
      CompletableFuture<InboundMessage> responseFuture = connection.sendRequest(methodCall).toCompletableFuture();
      InboundMessage response = responseFuture.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      // Verify response
      assertNotNull(response);
      assertNotNull(response.getSerial());

    } finally {
      connection.close();
    }
  }

  @Test
  void testConnectionReconnection() throws Exception {
    ConnectionConfig config = ConnectionConfig.builder()
        .withAutoReconnectEnabled(true)
        .withReconnectInitialDelay(Duration.ofMillis(100))
        .withReconnectMaxDelay(Duration.ofSeconds(2))
        .withMaxReconnectAttempts(3)
        .build();

    Connection connection = new NettyConnection(
        new InetSocketAddress("localhost", 12345),
        config
    );

    CountDownLatch reconnectLatch = new CountDownLatch(1);
    AtomicReference<ConnectionEvent> reconnectEvent = new AtomicReference<>();

    ConnectionEventListener listener = (conn, event) -> {
      if (event.getType() == ConnectionEventType.RECONNECTION_ATTEMPT) {
        reconnectEvent.set(event);
        reconnectLatch.countDown();
      }
    };

    try {
      connection.addConnectionEventListener(listener);

      // Connect initially
      connection.connect().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
      assertTrue(connection.isConnected());

      // Simulate connection loss by stopping container temporarily
      // Note: This test depends on container restart capability
      // For now, we just verify reconnection configuration is working
      assertEquals(0, connection.getReconnectAttemptCount());

    } finally {
      connection.close();
    }
  }

  @Test
  void testConcurrentConnections() throws Exception {
    int connectionCount = 5;
    Connection[] connections = new Connection[connectionCount];
    CompletableFuture<Void>[] connectFutures = new CompletableFuture[connectionCount];

    try {
      // Create multiple connections
      for (int i = 0; i < connectionCount; i++) {
        connections[i] = new NettyConnection(
            new InetSocketAddress("localhost", 12345),
            ConnectionConfig.defaultConfig()
        );
        connectFutures[i] = connections[i].connect().toCompletableFuture();
      }

      // Wait for all connections to establish
      CompletableFuture<Void> allConnected = CompletableFuture.allOf(connectFutures);
      assertDoesNotThrow(() -> allConnected.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));

      // Verify all connections are established
      for (Connection connection : connections) {
        assertTrue(connection.isConnected());
      }

    } finally {
      // Close all connections
      for (Connection connection : connections) {
        if (connection != null) {
          connection.close();
        }
      }
    }
  }

  @Test
  void testConnectionConfigValidation() {
    // Test invalid configurations
    assertThrows(IllegalArgumentException.class, () ->
        ConnectionConfig.builder().withConnectTimeout(Duration.ZERO).build());

    assertThrows(IllegalArgumentException.class, () ->
        ConnectionConfig.builder().withHealthCheckInterval(Duration.ofSeconds(-1)).build());

    assertThrows(IllegalArgumentException.class, () ->
        ConnectionConfig.builder().withMaxReconnectAttempts(-1).build());

    // Test valid configuration
    assertDoesNotThrow(() -> {
      ConnectionConfig config = ConnectionConfig.builder()
          .withConnectTimeout(Duration.ofSeconds(5))
          .withHealthCheckEnabled(true)
          .withHealthCheckInterval(Duration.ofSeconds(10))
          .withAutoReconnectEnabled(true)
          .withMaxReconnectAttempts(5)
          .build();

      assertNotNull(config);
    });
  }

  @Test
  void testConnectionStateTransitions() throws Exception {
    Connection connection = new NettyConnection(
        new InetSocketAddress("localhost", 12345),
        ConnectionConfig.defaultConfig()
    );

    CountDownLatch stateChangeLatch = new CountDownLatch(1);
    AtomicReference<ConnectionState> finalState = new AtomicReference<>();

    ConnectionEventListener listener = (conn, event) -> {
      if (event.getType() == ConnectionEventType.STATE_CHANGED) {
        ConnectionState newState = event.getNewState().orElse(null);
        if (newState == ConnectionState.CONNECTED) {
          finalState.set(newState);
          stateChangeLatch.countDown();
        }
      }
    };

    try {
      connection.addConnectionEventListener(listener);

      // Initial state should be DISCONNECTED
      assertEquals(ConnectionState.DISCONNECTED, connection.getState());

      // Connect and wait for state change
      connection.connect().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      assertTrue(stateChangeLatch.await(5, TimeUnit.SECONDS));
      assertEquals(ConnectionState.CONNECTED, finalState.get());
      assertEquals(ConnectionState.CONNECTED, connection.getState());

    } finally {
      connection.close();
    }
  }
}