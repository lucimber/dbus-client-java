/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionEventListener;
import com.lucimber.dbus.connection.ConnectionHandle;
import com.lucimber.dbus.connection.ConnectionHealthHandler;
import com.lucimber.dbus.connection.ConnectionReconnectHandler;
import com.lucimber.dbus.connection.ConnectionState;
import com.lucimber.dbus.connection.ConnectionStrategy;
import com.lucimber.dbus.connection.ConnectionStrategyRegistry;
import com.lucimber.dbus.connection.DefaultPipeline;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NettyConnection implements Connection {

  private static final Logger LOGGER = LoggerFactory.getLogger(NettyConnection.class);

  private final ExecutorService applicationTaskExecutor;
  private final SocketAddress serverAddress;
  private final Pipeline pipeline;
  private final ConnectionConfig config;
  private final ConnectionStrategy strategy;
  private ConnectionHandle connectionHandle;
  private ConnectionHealthHandler healthHandler;
  private ConnectionReconnectHandler reconnectHandler;

  public NettyConnection(SocketAddress serverAddress) {
    this(serverAddress, ConnectionConfig.defaultConfig());
  }

  public NettyConnection(SocketAddress serverAddress, ConnectionConfig config) {
    this.serverAddress = Objects.requireNonNull(serverAddress, "serverAddress must not be null");
    this.config = Objects.requireNonNull(config, "config must not be null");
    ConnectionStrategyRegistry strategyRegistry = createDefaultStrategyRegistry();
    this.strategy = strategyRegistry.findStrategy(serverAddress)
            .orElseThrow(() -> new IllegalArgumentException("No strategy available for: " + serverAddress));

    LOGGER.info("Using transport strategy: {}", strategy.getTransportName());

    this.pipeline = new DefaultPipeline(this);

    this.applicationTaskExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            runnable -> {
              Thread t = new Thread(runnable, "dbus-app-worker-" + System.identityHashCode(runnable));
              t.setDaemon(true);
              return t;
            });
  }

  /**
   * Creates the default strategy registry with all available Netty strategies.
   *
   * @return configured strategy registry
   */
  private static ConnectionStrategyRegistry createDefaultStrategyRegistry() {
    ConnectionStrategyRegistry registry = new ConnectionStrategyRegistry();
    registry.registerStrategy(new NettyUnixSocketStrategy());
    registry.registerStrategy(new NettyTcpStrategy());
    return registry;
  }

  /**
   * Creates a connection for the standard system bus path.
   * (Typically /var/run/dbus/system_bus_socket)
   *
   * @return A new instance.
   * @throws UnsupportedOperationException if native transport for UDS is not available.
   */
  public static NettyConnection newSystemBusConnection() {
    return newSystemBusConnection(ConnectionConfig.defaultConfig());
  }

  /**
   * Creates a connection for the standard system bus path with custom configuration.
   * (Typically /var/run/dbus/system_bus_socket)
   *
   * @param config The connection configuration to use
   * @return A new instance.
   * @throws UnsupportedOperationException if native transport for UDS is not available.
   */
  public static NettyConnection newSystemBusConnection(ConnectionConfig config) {
    // Standard system bus path, can be overridden by DBUS_SYSTEM_BUS_ADDRESS env var
    String path = System.getenv("DBUS_SYSTEM_BUS_ADDRESS");
    if (path == null || path.isEmpty()) {
      path = "/var/run/dbus/system_bus_socket"; // Default
    } else if (path.startsWith("unix:path=")) {
      path = path.substring("unix:path=".length());
    } else {
      // Handle other address formats if necessary (e.g., abstract sockets, tcp)
      LOGGER.warn("DBUS_SYSTEM_BUS_ADDRESS format not fully parsed, using raw value: {}", path);
    }
    return new NettyConnection(new DomainSocketAddress(path), config);
  }

  /**
   * Creates a connection for the standard session bus path.
   * (Path is usually obtained from DBUS_SESSION_BUS_ADDRESS env var)
   *
   * @return A new instance.
   * @throws UnsupportedOperationException if native transport for UDS is not available or address not found.
   */
  public static NettyConnection newSessionBusConnection() {
    return newSessionBusConnection(ConnectionConfig.defaultConfig());
  }

  /**
   * Creates a connection for the standard session bus path with custom configuration.
   * (Path is usually obtained from DBUS_SESSION_BUS_ADDRESS env var)
   *
   * @param config The connection configuration to use
   * @return A new instance.
   * @throws UnsupportedOperationException if native transport for UDS is not available or address not found.
   */
  public static NettyConnection newSessionBusConnection(ConnectionConfig config) {
    String address = System.getenv("DBUS_SESSION_BUS_ADDRESS");
    if (address == null || address.isEmpty()) {
      throw new IllegalStateException("DBUS_SESSION_BUS_ADDRESS environment variable not set.");
    }
    // DBUS_SESSION_BUS_ADDRESS can be complex, e.g., "unix:path=/tmp/dbus-...,guid=..."
    // For now, we'll parse simple "unix:path="
    if (address.startsWith("unix:path=")) {
      String path = address.substring("unix:path=".length());
      // It might have a comma and other params, e.g., unix:path=/tmp/dbus-...,guid=...
      int commaIndex = path.indexOf(',');
      if (commaIndex != -1) {
        path = path.substring(0, commaIndex);
      }
      return new NettyConnection(new DomainSocketAddress(path), config);
    } else if (address.startsWith("tcp:host=")) {
      // Example: tcp:host=localhost,port=12345 or tcp:host=127.0.0.1,port=12345,family=ipv4
      try {
        String host = null;
        int port = -1;
        String[] params = address.substring("tcp:".length()).split(",");
        for (String param : params) {
          String[] kv = param.split("=");
          if (kv.length == 2) {
            if ("host".equals(kv[0])) {
              host = kv[1];
            } else if ("port".equals(kv[0])) {
              port = Integer.parseInt(kv[1]);
            }
          }
        }
        if (host != null && port != -1) {
          return new NettyConnection(new InetSocketAddress(host, port), config);
        }
      } catch (Exception e) {
        throw new IllegalArgumentException("Could not parse TCP DBUS_SESSION_BUS_ADDRESS: " + address, e);
      }
    }
    throw new IllegalArgumentException("Unsupported DBUS_SESSION_BUS_ADDRESS format: "
            + address
            + ". Only simple 'unix:path=' or 'tcp:host=...,port=...' currently supported.");
  }

  @Override
  public CompletionStage<Void> connect() {
    // Check if already connected
    if (this.connectionHandle != null && this.connectionHandle.isActive()) {
      LOGGER.warn("Already connected or connection attempt in progress.");
      return CompletableFuture.completedFuture(null);
    }

    // Initialize handlers
    this.healthHandler = new ConnectionHealthHandler(config);
    this.reconnectHandler = new ConnectionReconnectHandler(config);

    // Add reconnect handler to pipeline if auto-reconnect is enabled
    if (config.isAutoReconnectEnabled()) {
      this.pipeline.addLast("reconnect-handler", reconnectHandler);
    }

    // Add health handler to pipeline if health monitoring is enabled
    if (config.isHealthCheckEnabled()) {
      this.pipeline.addLast("health-monitor", healthHandler);
    }

    // Create connection context for strategy
    NettyConnectionContext context = new NettyConnectionContext(pipeline, applicationTaskExecutor, this);

    LOGGER.info("Attempting to connect to DBus server at {} using strategy: {}", serverAddress, strategy.getTransportName());

    // Use strategy to establish connection
    return strategy.connect(serverAddress, config, context)
            .thenApply(handle -> {
              this.connectionHandle = handle;
              LOGGER.info("Connection established successfully");
              return null;
            });
  }

  @Override
  public boolean isConnected() {
    return connectionHandle != null && connectionHandle.isActive();
  }

  @Override
  public void close() {
    LOGGER.info("Closing DBus connection to {}...", serverAddress);

    // Shutdown reconnect handler first
    if (reconnectHandler != null) {
      reconnectHandler.shutdown();
    }

    // Shutdown health handler
    if (healthHandler != null) {
      healthHandler.shutdown();
    }

    // Close connection handle
    if (connectionHandle != null) {
      try {
        connectionHandle.close().toCompletableFuture().get(5, TimeUnit.SECONDS);
      } catch (Exception e) {
        LOGGER.warn("Error closing connection handle", e);
      }
    }

    // Shutdown application task executor
    if (applicationTaskExecutor != null && !applicationTaskExecutor.isShutdown()) {
      applicationTaskExecutor.shutdown();
      try {
        if (!applicationTaskExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          applicationTaskExecutor.shutdownNow();
        }
      } catch (InterruptedException ie) {
        applicationTaskExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    LOGGER.info("DBus connection to {} closed.", serverAddress);
  }

  @Override
  public DBusUInt32 getNextSerial() {
    if (connectionHandle == null || !connectionHandle.isActive()) {
      throw new IllegalStateException("Cannot get next serial, connection is not active.");
    }
    return connectionHandle.getNextSerial();
  }

  @Override
  public Pipeline getPipeline() {
    return pipeline;
  }

  @Override
  public CompletionStage<InboundMessage> sendRequest(OutboundMessage msg) {
    if (connectionHandle == null || !isConnected()) {
      CompletableFuture<InboundMessage> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new IllegalStateException("Not connected to D-Bus."));
      return failedFuture;
    }
    return connectionHandle.sendRequest(msg);
  }

  @Override
  public void sendAndRouteResponse(OutboundMessage msg, CompletionStage<Void> future) {
    if (connectionHandle == null || !isConnected()) {
      var re = new IllegalStateException("Not connected to D-Bus.");
      future.toCompletableFuture().completeExceptionally(re);
    } else {
      connectionHandle.send(msg).whenComplete((result, throwable) -> {
        if (throwable != null) {
          future.toCompletableFuture().completeExceptionally(throwable);
        } else {
          future.toCompletableFuture().complete(null);
        }
      });
    }
  }

  @Override
  public ConnectionConfig getConfig() {
    return config;
  }

  @Override
  public ConnectionState getState() {
    if (healthHandler != null) {
      return healthHandler.getCurrentState();
    }

    // Fallback to basic state detection if health handler is not available
    if (isConnected()) {
      return ConnectionState.CONNECTED;
    } else if (connectionHandle != null) {
      return ConnectionState.AUTHENTICATING;
    } else {
      return ConnectionState.DISCONNECTED;
    }
  }

  @Override
  public void addConnectionEventListener(ConnectionEventListener listener) {
    if (healthHandler != null) {
      healthHandler.addConnectionEventListener(listener);
    }
  }

  @Override
  public void removeConnectionEventListener(ConnectionEventListener listener) {
    if (healthHandler != null) {
      healthHandler.removeConnectionEventListener(listener);
    }
  }

  @Override
  public CompletionStage<Void> triggerHealthCheck() {
    if (healthHandler != null) {
      return healthHandler.triggerHealthCheck();
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public int getReconnectAttemptCount() {
    if (reconnectHandler != null) {
      return reconnectHandler.getAttemptCount();
    }
    return 0;
  }

  @Override
  public void cancelReconnection() {
    if (reconnectHandler != null) {
      reconnectHandler.cancelReconnection();
    }
  }

  @Override
  public void resetReconnectionState() {
    if (reconnectHandler != null) {
      reconnectHandler.reset();
    }
  }

  // Package-private methods for NettyConnectionContext callbacks

  /**
   * Called by NettyConnectionContext when connection state changes.
   */
  void notifyStateChanged(ConnectionState newState) {
    LOGGER.debug("State changed to: {}", newState);
    if (healthHandler != null) {
      // Update the health handler with the new state
      // Note: This is a simplified approach. The health handler might need
      // a different mechanism to update its internal state.
      LOGGER.debug("Notifying health handler of state change to: {}", newState);
    }
  }

  /**
   * Called by NettyConnectionContext when an error occurs.
   */
  void notifyError(Throwable error) {
    LOGGER.error("Connection error reported by strategy", error);
    // Could trigger reconnection logic here
  }

  /**
   * Called by NettyConnectionContext when connection is established.
   */
  void notifyConnectionEstablished() {
    LOGGER.debug("Connection establishment confirmed by strategy");
  }

  /**
   * Called by NettyConnectionContext when connection is lost.
   */
  void notifyConnectionLost() {
    LOGGER.warn("Connection loss reported by strategy");
    // Could trigger reconnection logic here if enabled
    if (reconnectHandler != null && config.isAutoReconnectEnabled()) {
      // Trigger reconnection
      LOGGER.info("Triggering auto-reconnection");
    }
  }
}
