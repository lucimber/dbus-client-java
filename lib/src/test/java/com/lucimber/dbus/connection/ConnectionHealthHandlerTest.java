/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionHealthHandlerTest {

  private ConnectionHealthHandler handler;
  private ConnectionConfig config;
  private Context mockContext;
  private Connection mockConnection;

  @BeforeEach
  void setUp() {
  config = ConnectionConfig.builder()
      .withHealthCheckEnabled(true)
      .withHealthCheckInterval(Duration.ofMillis(100))
      .withHealthCheckTimeout(Duration.ofMillis(50))
      .build();
  
  mockContext = mock(Context.class);
  mockConnection = mock(Connection.class);
  when(mockContext.getConnection()).thenReturn(mockConnection);
  
  handler = new ConnectionHealthHandler(config);
  }

  @AfterEach
  void tearDown() {
  if (handler != null) {
      handler.shutdown();
  }
  }

  @Test
  void testInitialState() {
  assertEquals(ConnectionState.DISCONNECTED, handler.getCurrentState());
  assertNull(handler.getLastSuccessfulCheck());
  }

  @Test
  void testStateTransition() {
  // Test state change from DISCONNECTED to CONNECTED
  handler.onConnectionActive(mockContext);
  assertEquals(ConnectionState.CONNECTED, handler.getCurrentState());
  
  // Test state change from CONNECTED to DISCONNECTED
  handler.onConnectionInactive(mockContext);
  assertEquals(ConnectionState.DISCONNECTED, handler.getCurrentState());
  }

  @Test
  void testEventListener() throws InterruptedException {
  CountDownLatch eventLatch = new CountDownLatch(1);
  AtomicReference<ConnectionEvent> receivedEvent = new AtomicReference<>();
  
  ConnectionEventListener listener = (connection, event) -> {
      receivedEvent.set(event);
      eventLatch.countDown();
  };
  
  handler.addConnectionEventListener(listener);
  handler.onHandlerAdded(mockContext);
  handler.onConnectionActive(mockContext);
  
  // Wait for event to be fired
  assertTrue(eventLatch.await(1, TimeUnit.SECONDS));
  
  ConnectionEvent event = receivedEvent.get();
  assertNotNull(event);
  assertEquals(ConnectionEventType.STATE_CHANGED, event.getType());
  assertEquals(ConnectionState.DISCONNECTED, event.getOldState().orElse(null));
  assertEquals(ConnectionState.CONNECTED, event.getNewState().orElse(null));
  }

  @Test
  void testHealthCheckTrigger() {
  handler.onHandlerAdded(mockContext);
  
  // Should complete immediately if not active
  assertDoesNotThrow(() -> {
      handler.triggerHealthCheck().get(100, TimeUnit.MILLISECONDS);
  });
  
  // After connection becomes active, should attempt health check
  handler.onConnectionActive(mockContext);
  assertDoesNotThrow(() -> {
      handler.triggerHealthCheck().get(200, TimeUnit.MILLISECONDS);
  });
  }

  @Test
  void testListenerManagement() {
  ConnectionEventListener listener = mock(ConnectionEventListener.class);
  
  handler.addConnectionEventListener(listener);
  handler.removeConnectionEventListener(listener);
  
  // After removal, listener should not receive events
  handler.onHandlerAdded(mockContext);
  handler.onConnectionActive(mockContext);
  
  // Give some time for events to be processed
  try {
      Thread.sleep(100);
  } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
  }
  
  verifyNoInteractions(listener);
  }

  @Test
  void testConfigurationRespected() {
  // Test with health check disabled
  ConnectionConfig disabledConfig = ConnectionConfig.builder()
      .withHealthCheckEnabled(false)
      .build();
  
  ConnectionHealthHandler disabledHandler = new ConnectionHealthHandler(disabledConfig);
  
  try {
      disabledHandler.onHandlerAdded(mockContext);
      disabledHandler.onConnectionActive(mockContext);
      
      assertEquals(ConnectionState.CONNECTED, disabledHandler.getCurrentState());
      
      // Should complete immediately since health check is disabled
      assertDoesNotThrow(() -> {
    disabledHandler.triggerHealthCheck().get(100, TimeUnit.MILLISECONDS);
      });
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
  
  // State should remain as is after shutdown
  assertEquals(ConnectionState.CONNECTED, handler.getCurrentState());
  }
}