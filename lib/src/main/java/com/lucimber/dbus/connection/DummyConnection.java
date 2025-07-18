/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A dummy implementation of {@link Connection} for testing D-Bus applications without requiring
 * a real D-Bus daemon, similar to Netty's {@code EmbeddedChannel}.
 *
 * <p>This class provides a complete implementation of the {@link Connection} interface that can be
 * used in unit tests and integration tests. It simulates D-Bus behavior including connection
 * lifecycle, message handling, and error conditions.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>No D-Bus daemon required</strong> - Works entirely in-memory</li>
 *   <li><strong>Configurable responses</strong> - Define custom responses for method calls</li>
 *   <li><strong>Connection lifecycle simulation</strong> - Realistic state transitions</li>
 *   <li><strong>Message capture</strong> - Inspect sent messages for verification</li>
 *   <li><strong>Error simulation</strong> - Test connection failures and recovery</li>
 *   <li><strong>Thread-safe</strong> - Safe for concurrent testing</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * @Test
 * public void testMyService() {
 *     // Create a dummy connection
 *     DummyConnection connection = DummyConnection.create();
 *
 *     // Define a custom response
 *     connection.setMethodCallResponse("com.example.Service", "GetData",
 *         DummyConnection.successResponse(List.of(DBusString.valueOf("test-data"))));
 *
 *     // Connect and use in your service
 *     connection.connect().toCompletableFuture().get();
 *     MyService service = new MyService(connection);
 *
 *     // Test the service
 *     String result = service.getData();
 *     assertEquals("test-data", result);
 *
 *     // Verify the method was called
 *     assertTrue(connection.wasMethodCalled("com.example.Service", "GetData"));
 * }
 * }</pre>
 *
 * <h2>Advanced Testing</h2>
 * <pre>{@code
 * @Test
 * public void testConnectionFailure() {
 *     DummyConnection connection = DummyConnection.builder()
 *         .withConnectionFailure(true)
 *         .build();
 *
 *     // Test how your code handles connection failures
 *     assertThrows(Exception.class, () -> connection.connect().toCompletableFuture().get());
 * }
 *
 * @Test
 * public void testMessageCapture() {
 *     DummyConnection connection = DummyConnection.create();
 *     connection.connect().toCompletableFuture().get();
 *
 *     // Your code sends messages
 *     myService.performAction();
 *
 *     // Verify messages were sent
 *     List<OutboundMessage> messages = connection.getSentMessages();
 *     assertEquals(1, messages.size());
 *
 *     OutboundMethodCall call = (OutboundMethodCall) messages.get(0);
 *     assertEquals("PerformAction", call.getMember().toString());
 * }
 * }</pre>
 *
 * <h2>Error Testing</h2>
 * <pre>{@code
 * @Test
 * public void testErrorHandling() {
 *     DummyConnection connection = DummyConnection.create();
 *     connection.setMethodCallResponse("com.example.Service", "FailingMethod",
 *         DummyConnection.errorResponse("com.example.Error", "Something went wrong"));
 *
 *     connection.connect().toCompletableFuture().get();
 *
 *     // Test that your code handles D-Bus errors properly
 *     assertThrows(MyServiceException.class, () -> myService.callFailingMethod());
 * }
 * }</pre>
 *
 * <h2>Connection Events</h2>
 * <pre>{@code
 * @Test
 * public void testConnectionEvents() {
 *     DummyConnection connection = DummyConnection.create();
 *     AtomicBoolean connected = new AtomicBoolean(false);
 *
 *     connection.addConnectionEventListener((conn, event) -> {
 *         if (event.getType() == ConnectionEventType.STATE_CHANGED
 *             && event.getNewState().orElse(null) == ConnectionState.CONNECTED) {
 *             connected.set(true);
 *         }
 *     });
 *
 *     connection.connect().toCompletableFuture().get();
 *     assertTrue(connected.get());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe and can be used in concurrent tests. All methods can be called
 * from multiple threads without external synchronization.
 *
 * <h2>Cleanup</h2>
 * <p>Always call {@link #close()} when done testing to clean up resources:
 * <pre>{@code
 * @Test
 * public void testWithCleanup() {
 *     DummyConnection connection = DummyConnection.create();
 *     try {
 *         // Your test code
 *     } finally {
 *         connection.close();
 *     }
 * }
 * }</pre>
 *
 * @since 2.0
 */
public class DummyConnection implements Connection {

  private final AtomicInteger serialCounter = new AtomicInteger(1);
  private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
  private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
  private final DummyPipeline pipeline;
  private final ConnectionConfig config;
  private final CopyOnWriteArrayList<ConnectionEventListener> listeners = new CopyOnWriteArrayList<>();
  private final Map<String, Function<OutboundMessage, InboundMessage>> responseHandlers = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
    Thread t = new Thread(r, "DummyConnection-Scheduler");
    t.setDaemon(true);
    return t;
  });

  // Message capture for testing
  private final BlockingQueue<OutboundMessage> sentMessages = new LinkedBlockingQueue<>();
  private final BlockingQueue<ConnectionEvent> connectionEvents = new LinkedBlockingQueue<>();

  // Test configuration
  private final Duration connectDelay;
  private final boolean shouldFailConnection;
  private final boolean shouldFailHealthCheck;
  private volatile boolean closed = false;
  private volatile CompletableFuture<Void> currentHealthCheck;

  private DummyConnection(Builder builder) {
    this.config = builder.config;
    this.connectDelay = builder.connectDelay;
    this.shouldFailConnection = builder.shouldFailConnection;
    this.shouldFailHealthCheck = builder.shouldFailHealthCheck;
    this.pipeline = new DummyPipeline(this);

    // Set up response handlers
    this.responseHandlers.putAll(builder.responseHandlers);
    setupDefaultResponses();
  }

  /**
   * Creates a new builder for configuring a DummyConnection.
   *
   * @return a new Builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a simple DummyConnection with default configuration.
   *
   * @return a new DummyConnection instance ready for use
   */
  public static DummyConnection create() {
    return new Builder().build();
  }

  // Connection interface implementation

  @Override
  public CompletionStage<Void> connect() {
    if (closed) {
      var e = new IllegalStateException("Connection is closed");
      return CompletableFuture.failedFuture(e);
    }

    if (state.compareAndSet(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING)) {
      CompletableFuture<Void> connectFuture = new CompletableFuture<>();

      // Fire connecting event
      fireConnectionEvent(ConnectionEvent
              .stateChanged(ConnectionState.DISCONNECTED, ConnectionState.CONNECTING));

      scheduler.schedule(() -> {
        try {
          if (shouldFailConnection) {
            ConnectionState oldState = state.getAndSet(ConnectionState.FAILED);
            var e = new RuntimeException("Simulated connection failure");
            connectFuture.completeExceptionally(e);
            fireConnectionEvent(ConnectionEvent.stateChanged(oldState, ConnectionState.FAILED));
          } else {
            // Transition to authenticating
            state.set(ConnectionState.AUTHENTICATING);
            fireConnectionEvent(ConnectionEvent
                    .stateChanged(ConnectionState.CONNECTING, ConnectionState.AUTHENTICATING));

            // Simulate authentication delay
            scheduler.schedule(() -> {
              ConnectionState oldState = state.getAndSet(ConnectionState.CONNECTED);
              connectFuture.complete(null);
              fireConnectionEvent(ConnectionEvent.stateChanged(oldState, ConnectionState.CONNECTED));
            }, connectDelay.toMillis() / 2, TimeUnit.MILLISECONDS);
          }
        } catch (Exception e) {
          ConnectionState oldState = state.getAndSet(ConnectionState.FAILED);
          connectFuture.completeExceptionally(e);
          fireConnectionEvent(ConnectionEvent.stateChanged(oldState, ConnectionState.FAILED));
        }
      }, connectDelay.toMillis(), TimeUnit.MILLISECONDS);

      return connectFuture;
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public boolean isConnected() {
    return state.get() == ConnectionState.CONNECTED;
  }

  @Override
  public Pipeline getPipeline() {
    return pipeline;
  }

  @Override
  public DBusUInt32 getNextSerial() {
    return DBusUInt32.valueOf(serialCounter.getAndIncrement());
  }

  @Override
  public CompletionStage<InboundMessage> sendRequest(OutboundMessage msg) {
    if (!state.get().canHandleRequests()) {
      var e = new IllegalStateException("Connection not ready: " + state.get());
      return CompletableFuture.failedFuture(e);
    }

    // Capture the message for testing
    sentMessages.offer(msg);

    return CompletableFuture.supplyAsync(() -> {
      // Simulate network delay
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Interrupted", e);
      }

      return generateResponse(msg);
    }, scheduler);
  }

  @Override
  public void sendAndRouteResponse(OutboundMessage msg, CompletionStage<Void> future) {
    if (!state.get().canHandleRequests()) {
      var e = new IllegalStateException("Connection not ready: " + state.get());
      future.toCompletableFuture().completeExceptionally(e);
      return;
    }

    // Capture the message for testing
    sentMessages.offer(msg);

    // Simulate sending the message
    scheduler.schedule(() -> {
      try {
        future.toCompletableFuture().complete(null);
      } catch (Exception e) {
        future.toCompletableFuture().completeExceptionally(e);
      }
    }, 5, TimeUnit.MILLISECONDS);
  }

  @Override
  public ConnectionConfig getConfig() {
    return config;
  }

  @Override
  public ConnectionState getState() {
    return state.get();
  }

  @Override
  public void addConnectionEventListener(ConnectionEventListener listener) {
    listeners.add(Objects.requireNonNull(listener));
  }

  @Override
  public void removeConnectionEventListener(ConnectionEventListener listener) {
    listeners.remove(listener);
  }

  @Override
  public CompletionStage<Void> triggerHealthCheck() {
    if (currentHealthCheck != null && !currentHealthCheck.isDone()) {
      return currentHealthCheck;
    }

    currentHealthCheck = new CompletableFuture<>();

    scheduler.schedule(() -> {
      if (shouldFailHealthCheck) {
        ConnectionState oldState = state.getAndSet(ConnectionState.UNHEALTHY);
        currentHealthCheck.completeExceptionally(new RuntimeException("Health check failed"));
        fireConnectionEvent(ConnectionEvent
                .healthCheckFailure(new RuntimeException("Simulated health check failure")));
        if (oldState != ConnectionState.UNHEALTHY) {
          fireConnectionEvent(ConnectionEvent.stateChanged(oldState, ConnectionState.UNHEALTHY));
        }
      } else {
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.UNHEALTHY) {
          state.set(ConnectionState.CONNECTED);
          fireConnectionEvent(ConnectionEvent.stateChanged(ConnectionState.UNHEALTHY, ConnectionState.CONNECTED));
        }
        currentHealthCheck.complete(null);
        fireConnectionEvent(ConnectionEvent.healthCheckSuccess());
      }
    }, 10, TimeUnit.MILLISECONDS);

    return currentHealthCheck;
  }

  @Override
  public int getReconnectAttemptCount() {
    return reconnectAttempts.get();
  }

  @Override
  public void cancelReconnection() {
    // For testing, we just simulate the event
  }

  @Override
  public void resetReconnectionState() {
    reconnectAttempts.set(0);
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    closed = true;
    ConnectionState oldState = state.getAndSet(ConnectionState.DISCONNECTED);

    if (oldState != ConnectionState.DISCONNECTED) {
      fireConnectionEvent(ConnectionEvent.stateChanged(oldState, ConnectionState.DISCONNECTED));
    }

    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  // Testing utility methods

  /**
   * Sets a response handler for method calls to a specific interface and method.
   *
   * @param interfaceName    the D-Bus interface name
   * @param methodName       the method name
   * @param responseFunction function to generate the response
   */
  public void setMethodCallResponse(String interfaceName, String methodName,
                                    Function<OutboundMessage, InboundMessage> responseFunction) {
    responseHandlers.put(interfaceName + "." + methodName, responseFunction);
  }

  /**
   * Checks if a method call was made to the specified interface and method.
   *
   * @param interfaceName the D-Bus interface name
   * @param methodName    the method name
   * @return true if the method was called
   */
  public boolean wasMethodCalled(String interfaceName, String methodName) {
    return sentMessages.stream()
            .filter(OutboundMethodCall.class::isInstance)
            .map(OutboundMethodCall.class::cast)
            .anyMatch(call ->
                    call.getInterfaceName().map(DBusString::toString).orElse("").equals(interfaceName) &&
                            call.getMember().toString().equals(methodName));
  }

  /**
   * Returns the number of times a method was called.
   *
   * @param interfaceName the D-Bus interface name
   * @param methodName    the method name
   * @return the number of times the method was called
   */
  public int getMethodCallCount(String interfaceName, String methodName) {
    return (int) sentMessages.stream()
            .filter(OutboundMethodCall.class::isInstance)
            .map(OutboundMethodCall.class::cast)
            .filter(call ->
                    call.getInterfaceName().map(DBusString::toString).orElse("").equals(interfaceName) &&
                            call.getMember().toString().equals(methodName))
            .count();
  }

  /**
   * Returns all messages sent through this connection.
   *
   * @return a list of sent messages (safe to modify)
   */
  public List<OutboundMessage> getSentMessages() {
    return new ArrayList<>(sentMessages);
  }

  /**
   * Returns all messages sent through this connection that match the given predicate.
   *
   * @param predicate the predicate to filter messages
   * @return a list of matching messages
   */
  public List<OutboundMessage> getSentMessages(Predicate<OutboundMessage> predicate) {
    return sentMessages.stream()
            .filter(predicate)
            .toList();
  }

  /**
   * Returns all method calls sent to a specific interface.
   *
   * @param interfaceName the D-Bus interface name
   * @return a list of method calls to the interface
   */
  public List<OutboundMethodCall> getMethodCalls(String interfaceName) {
    return sentMessages.stream()
            .filter(OutboundMethodCall.class::isInstance)
            .map(OutboundMethodCall.class::cast)
            .filter(call -> call.getInterfaceName()
                    .map(DBusString::toString).orElse("").equals(interfaceName))
            .toList();
  }

  /**
   * Returns all connection events that have occurred.
   *
   * @return a list of connection events (safe to modify)
   */
  public List<ConnectionEvent> getConnectionEvents() {
    return new ArrayList<>(connectionEvents);
  }

  /**
   * Waits for a specific connection event to occur.
   *
   * @param eventType the type of event to wait for
   * @param timeout   the maximum time to wait
   * @param unit      the time unit of the timeout argument
   * @return true if the event occurred, false if timeout
   * @throws InterruptedException if the current thread is interrupted
   */
  public boolean waitForEvent(ConnectionEventType eventType, long timeout, TimeUnit unit)
          throws InterruptedException {
    long endTime = System.nanoTime() + unit.toNanos(timeout);

    while (System.nanoTime() < endTime) {
      ConnectionEvent event = connectionEvents.poll(100, TimeUnit.MILLISECONDS);
      if (event != null && event.getType() == eventType) {
        return true;
      }
    }
    return false;
  }

  /**
   * Clears all captured messages and events.
   */
  public void clearCaptures() {
    sentMessages.clear();
    connectionEvents.clear();
  }

  /**
   * Simulates a connection failure by transitioning to FAILED state.
   */
  public void simulateConnectionFailure() {
    ConnectionState oldState = state.getAndSet(ConnectionState.FAILED);
    if (oldState.canHandleRequests()) {
      fireConnectionEvent(ConnectionEvent.stateChanged(oldState, ConnectionState.FAILED));
    }
  }

  /**
   * Simulates a reconnection attempt.
   */
  public void simulateReconnection() {
    if (state.compareAndSet(ConnectionState.FAILED, ConnectionState.RECONNECTING)) {
      int attempts = reconnectAttempts.incrementAndGet();
      fireConnectionEvent(ConnectionEvent.reconnectionAttempt(attempts));

      scheduler.schedule(() -> {
        ConnectionState oldState = state.getAndSet(ConnectionState.CONNECTED);
        fireConnectionEvent(ConnectionEvent.stateChanged(oldState, ConnectionState.CONNECTED));
      }, connectDelay.toMillis(), TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Creates a success response with the given body.
   *
   * @param body the response body
   * @return a function that creates a success response
   */
  public static Function<OutboundMessage, InboundMessage> successResponse(List<DBusType> body) {
    return msg -> {
      if (msg instanceof OutboundMethodCall call) {
        return InboundMethodReturn.Builder.create()
                .withSerial(DBusUInt32.valueOf((int) (System.nanoTime() % Integer.MAX_VALUE)))
                .withReplySerial(call.getSerial())
                .withSender(DBusString.valueOf(":1.0"))
                .withBody(call.getSignature().orElse(DBusSignature.valueOf("")), body)
                .build();
      }
      throw new IllegalArgumentException("Unsupported message type: " + msg.getClass());
    };
  }

  /**
   * Creates an error response with the given error name and message.
   *
   * @param errorName    the D-Bus error name
   * @param errorMessage the error message
   * @return a function that creates an error response
   */
  public static Function<OutboundMessage, InboundMessage> errorResponse(String errorName, String errorMessage) {
    return msg -> {
      if (msg instanceof OutboundMethodCall call) {
        return InboundError.Builder.create()
                .withSerial(DBusUInt32.valueOf((int) (System.nanoTime() % Integer.MAX_VALUE)))
                .withReplySerial(call.getSerial())
                .withSender(DBusString.valueOf(":1.0"))
                .withErrorName(DBusString.valueOf(errorName))
                .withBody(DBusSignature.valueOf("s"), List.of(DBusString.valueOf(errorMessage)))
                .build();
      }
      throw new IllegalArgumentException("Unsupported message type: " + msg.getClass());
    };
  }

  private void setupDefaultResponses() {
    // Default response for D-Bus introspection
    setMethodCallResponse("org.freedesktop.DBus.Introspectable", "Introspect",
            successResponse(List.of(DBusString.valueOf("<!DOCTYPE node PUBLIC \"-//freedesktop//DTD"
                    + " D-BUS Object Introspection 1.0//EN\""
                    + " \"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\"><node></node>"))));

    // Default response for D-Bus Peer interface
    setMethodCallResponse("org.freedesktop.DBus.Peer", "Ping",
            successResponse(List.of()));

    setMethodCallResponse("org.freedesktop.DBus.Peer", "GetMachineId",
            successResponse(List.of(DBusString.valueOf("dummy-machine-id-" + System.nanoTime()))));
  }

  private InboundMessage generateResponse(OutboundMessage msg) {
    if (msg instanceof OutboundMethodCall call) {
      String interfaceName = call.getInterfaceName()
              .map(DBusString::toString)
              .orElse("unknown");
      String methodName = call.getMember().toString();
      String key = interfaceName + "." + methodName;

      Function<OutboundMessage, InboundMessage> handler = responseHandlers.get(key);
      if (handler != null) {
        return handler.apply(msg);
      }

      // Default error response for unknown methods
      return errorResponse("org.freedesktop.DBus.Error.UnknownMethod",
              "Unknown method: " + methodName + " on interface: " + interfaceName).apply(msg);
    }

    throw new IllegalArgumentException("Unsupported message type: " + msg.getClass());
  }

  private void fireConnectionEvent(ConnectionEvent event) {
    connectionEvents.offer(event);

    for (ConnectionEventListener listener : listeners) {
      try {
        listener.onConnectionEvent(this, event);
      } catch (Exception e) {
        // Log and continue with other listeners
        System.err.println("Error in connection event listener: " + e.getMessage());
      }
    }
  }

  /**
   * Simple pipeline implementation for testing purposes.
   */
  private static class DummyPipeline implements Pipeline {
    private final Connection connection;
    private final CopyOnWriteArrayList<HandlerEntry> handlers = new CopyOnWriteArrayList<>();
    private final Map<String, HandlerEntry> handlerMap = new ConcurrentHashMap<>();

    private DummyPipeline(Connection connection) {
      this.connection = connection;
    }

    @Override
    public Pipeline addLast(String name, Handler handler) {
      Objects.requireNonNull(name, "Handler name cannot be null");
      Objects.requireNonNull(handler, "Handler cannot be null");

      HandlerEntry entry = new HandlerEntry(name, handler);

      if (handlerMap.putIfAbsent(name, entry) != null) {
        throw new IllegalArgumentException("Handler with name '" + name + "' already exists");
      }

      handlers.add(entry);
      return this;
    }

    @Override
    public Connection getConnection() {
      return connection;
    }

    @Override
    public Pipeline remove(String name) {
      Objects.requireNonNull(name, "Handler name cannot be null");

      HandlerEntry entry = handlerMap.remove(name);
      if (entry == null) {
        throw new IllegalArgumentException("No handler with name '" + name + "' exists");
      }

      handlers.remove(entry);
      return this;
    }

    @Override
    public void propagateInboundMessage(InboundMessage msg) {
      // For testing, we don't need complex propagation
    }

    @Override
    public void propagateOutboundMessage(OutboundMessage msg, CompletableFuture<Void> future) {
      // For testing, we don't need complex propagation
      future.complete(null);
    }

    @Override
    public void propagateConnectionActive() {
      // For testing, we don't need complex propagation
    }

    @Override
    public void propagateConnectionInactive() {
      // For testing, we don't need complex propagation
    }

    @Override
    public void propagateInboundFailure(Throwable cause) {
      // For testing, we don't need complex propagation
    }

    private record HandlerEntry(String name, Handler handler) {

      @Override
          public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            HandlerEntry that = (HandlerEntry) obj;
            return Objects.equals(name, that.name);
          }

          @Override
          public int hashCode() {
            return Objects.hash(name);
          }
        }
  }

  /**
   * Builder for creating DummyConnection instances with custom configuration.
   */
  public static class Builder {
    private ConnectionConfig config = ConnectionConfig.builder().build();
    private Duration connectDelay = Duration.ofMillis(50);
    private boolean shouldFailConnection = false;
    private boolean shouldFailHealthCheck = false;
    private final Map<String, Function<OutboundMessage, InboundMessage>> responseHandlers =
            new ConcurrentHashMap<>();

    /**
     * Sets the connection configuration.
     *
     * @param config the connection configuration
     * @return this builder
     */
    public Builder withConfig(ConnectionConfig config) {
      this.config = Objects.requireNonNull(config);
      return this;
    }

    /**
     * Sets the simulated connection delay.
     *
     * @param delay the connection delay
     * @return this builder
     */
    public Builder withConnectDelay(Duration delay) {
      this.connectDelay = Objects.requireNonNull(delay);
      return this;
    }

    /**
     * Configures the connection to fail during connect.
     *
     * @param shouldFail true to simulate connection failure
     * @return this builder
     */
    public Builder withConnectionFailure(boolean shouldFail) {
      this.shouldFailConnection = shouldFail;
      return this;
    }

    /**
     * Configures health checks to fail.
     *
     * @param shouldFail true to simulate health check failure
     * @return this builder
     */
    public Builder withHealthCheckFailure(boolean shouldFail) {
      this.shouldFailHealthCheck = shouldFail;
      return this;
    }

    /**
     * Adds a response handler for method calls.
     *
     * @param interfaceName    the D-Bus interface name
     * @param methodName       the method name
     * @param responseFunction function to generate the response
     * @return this builder
     */
    public Builder withMethodCallResponse(String interfaceName, String methodName,
                                          Function<OutboundMessage, InboundMessage> responseFunction) {
      responseHandlers.put(interfaceName + "." + methodName, responseFunction);
      return this;
    }

    /**
     * Builds the DummyConnection instance.
     *
     * @return a new DummyConnection
     */
    public DummyConnection build() {
      return new DummyConnection(this);
    }
  }
}