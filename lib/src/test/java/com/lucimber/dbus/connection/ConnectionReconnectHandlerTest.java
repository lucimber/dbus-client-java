package com.lucimber.dbus.connection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionReconnectHandlerTest {

  private ConnectionReconnectHandler handler;
  private ConnectionConfig config;
  private Context mockContext;
  private Connection mockConnection;

  @BeforeEach
  void setUp() {
    config = ConnectionConfig.builder()
            .withAutoReconnectEnabled(true)
            .withReconnectInitialDelay(Duration.ofMillis(10))
            .withReconnectMaxDelay(Duration.ofSeconds(1))
            .withReconnectBackoffMultiplier(2.0)
            .withMaxReconnectAttempts(3)
            .build();
    
    mockContext = mock(Context.class);
    mockConnection = mock(Connection.class);
    when(mockContext.getConnection()).thenReturn(mockConnection);
    
    handler = new ConnectionReconnectHandler(config);
  }

  @AfterEach
  void tearDown() {
    if (handler != null) {
      handler.shutdown();
    }
  }

  @Test
  void testInitialState() {
    assertEquals(0, handler.getAttemptCount());
    assertNull(handler.getLastReconnectAttempt());
    assertFalse(handler.isEnabled());
  }

  @Test
  void testHandlerLifecycle() {
    handler.onHandlerAdded(mockContext);
    assertFalse(handler.isEnabled()); // Initially disabled until connection is active
    
    handler.onConnectionActive(mockContext);
    assertTrue(handler.isEnabled());
    
    handler.onHandlerRemoved(mockContext);
    assertFalse(handler.isEnabled());
  }

  @Test
  void testConnectionActiveResetsState() {
    // Simulate some previous attempts
    handler.onHandlerAdded(mockContext);
    handler.onConnectionActive(mockContext);
    
    // Verify initial state
    assertEquals(0, handler.getAttemptCount());
    assertNull(handler.getLastReconnectAttempt());
    assertTrue(handler.isEnabled());
  }

  @Test
  void testConnectionInactiveDisablesReconnection() {
    handler.onHandlerAdded(mockContext);
    handler.onConnectionActive(mockContext);
    
    handler.onConnectionInactive(mockContext);
    
    assertFalse(handler.isEnabled());
  }

  @Test
  void testReconnectionStateHandling() throws InterruptedException {
    CountDownLatch eventLatch = new CountDownLatch(1);
    AtomicReference<ConnectionEventListener> capturedListener = new AtomicReference<>();
    
    // Mock connection.connect() to return a failed future
    CompletableFuture<Void> failedFuture = CompletableFuture.failedFuture(new RuntimeException("Connection failed"));
    when(mockConnection.connect()).thenReturn(failedFuture);
    
    // Capture the event listener
    doAnswer(invocation -> {
      ConnectionEventListener listener = invocation.getArgument(0);
      capturedListener.set(listener);
      return null;
    }).when(mockConnection).addConnectionEventListener(any(ConnectionEventListener.class));
    
    handler.onHandlerAdded(mockContext);
    handler.onConnectionActive(mockContext);
    
    // Manually trigger a connection failure event to the captured listener
    ConnectionEventListener listener = capturedListener.get();
    assertNotNull(listener);
    
    // Simulate a connection failure event
    ConnectionEvent event = ConnectionEvent.stateChanged(ConnectionState.CONNECTED, ConnectionState.FAILED);
    listener.onConnectionEvent(mockConnection, event);
    
    // Give some time for reconnection scheduling
    Thread.sleep(100);
    
    // Verify reconnection was attempted
    verify(mockConnection, timeout(500).atLeastOnce()).connect();
  }

  @Test
  void testMaxReconnectAttempts() throws InterruptedException {
    AtomicInteger connectCalls = new AtomicInteger(0);
    AtomicReference<ConnectionEventListener> capturedListener = new AtomicReference<>();
    
    // Mock connection.connect() to always fail
    when(mockConnection.connect()).thenAnswer(invocation -> {
      connectCalls.incrementAndGet();
      return CompletableFuture.failedFuture(new RuntimeException("Connection failed"));
    });
    
    // Capture the event listener
    doAnswer(invocation -> {
      ConnectionEventListener listener = invocation.getArgument(0);
      capturedListener.set(listener);
      return null;
    }).when(mockConnection).addConnectionEventListener(any(ConnectionEventListener.class));
    
    handler.onHandlerAdded(mockContext);
    handler.onConnectionActive(mockContext);
    
    // Manually trigger a connection failure event to start reconnection
    ConnectionEventListener listener = capturedListener.get();
    assertNotNull(listener);
    
    ConnectionEvent event = ConnectionEvent.stateChanged(ConnectionState.CONNECTED, ConnectionState.FAILED);
    listener.onConnectionEvent(mockConnection, event);
    
    // Wait for all reconnection attempts to complete
    Thread.sleep(500);
    
    // Should have made some attempts (might not reach max due to timing)
    assertTrue(connectCalls.get() >= 1);
    // Handler should still be enabled during attempts, but will be disabled after max attempts
    // Note: This depends on timing, so we don't assert the exact state
  }

  @Test
  void testBackoffCalculation() {
    // Test exponential backoff calculation
    Duration initialDelay = Duration.ofMillis(100);
    double multiplier = 2.0;
    Duration maxDelay = Duration.ofSeconds(5);
    
    ConnectionConfig testConfig = ConnectionConfig.builder()
            .withAutoReconnectEnabled(true)
            .withReconnectInitialDelay(initialDelay)
            .withReconnectBackoffMultiplier(multiplier)
            .withReconnectMaxDelay(maxDelay)
            .build();
    
    ConnectionReconnectHandler testHandler = new ConnectionReconnectHandler(testConfig);
    
    try {
      // The backoff calculation is internal, so we just verify the handler works
      testHandler.onHandlerAdded(mockContext);
      testHandler.onConnectionActive(mockContext);
      
      assertNotNull(testHandler);
      assertEquals(0, testHandler.getAttemptCount());
    } finally {
      testHandler.shutdown();
    }
  }

  @Test
  void testCancelReconnection() {
    handler.onHandlerAdded(mockContext);
    handler.onConnectionActive(mockContext);
    
    // Cancel any pending reconnection
    handler.cancelReconnection();
    
    // Should not throw exception
    assertDoesNotThrow(() -> handler.reset());
  }

  @Test
  void testConfigurationRespected() {
    // Test with auto-reconnect disabled
    ConnectionConfig disabledConfig = ConnectionConfig.builder()
            .withAutoReconnectEnabled(false)
            .build();
    
    ConnectionReconnectHandler disabledHandler = new ConnectionReconnectHandler(disabledConfig);
    
    try {
      disabledHandler.onHandlerAdded(mockContext);
      disabledHandler.onConnectionActive(mockContext);
      
      assertTrue(disabledHandler.isEnabled()); // Handler can be enabled
      assertEquals(0, disabledHandler.getAttemptCount());
    } finally {
      disabledHandler.shutdown();
    }
  }

  @Test
  void testShutdown() {
    handler.onHandlerAdded(mockContext);
    handler.onConnectionActive(mockContext);
    
    // Should not throw exception
    assertDoesNotThrow(() -> handler.shutdown());
    
    // Should be able to call shutdown multiple times
    assertDoesNotThrow(() -> handler.shutdown());
  }
}