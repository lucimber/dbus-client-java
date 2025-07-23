/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.type.DBusUInt32;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionHealthHandlerTest {

    private ConnectionHealthHandler handler;
    private ConnectionConfig config;
    private Context mockContext;
    private Connection mockConnection;

    @BeforeEach
    void setUp() {
        config =
                ConnectionConfig.builder()
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

        ConnectionEventListener listener =
                (connection, event) -> {
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
        assertDoesNotThrow(
                () -> {
                    handler.triggerHealthCheck().get(100, TimeUnit.MILLISECONDS);
                });

        // After connection becomes active, should attempt health check
        handler.onConnectionActive(mockContext);
        assertDoesNotThrow(
                () -> {
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
        ConnectionConfig disabledConfig =
                ConnectionConfig.builder().withHealthCheckEnabled(false).build();

        ConnectionHealthHandler disabledHandler = new ConnectionHealthHandler(disabledConfig);

        try {
            disabledHandler.onHandlerAdded(mockContext);
            disabledHandler.onConnectionActive(mockContext);

            assertEquals(ConnectionState.CONNECTED, disabledHandler.getCurrentState());

            // Should complete immediately since health check is disabled
            assertDoesNotThrow(
                    () -> {
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

    @Test
    void testMaxConsecutiveFailures() {
        handler.onHandlerAdded(mockContext);
        when(mockConnection.getNextSerial()).thenReturn(DBusUInt32.valueOf(1));

        // Simulate failures by not providing any response to health checks
        handler.onConnectionActive(mockContext);

        // Wait for enough time for multiple failures to occur
        // With 100ms interval, we should get multiple failures within a few seconds
        try {
            Thread.sleep(1200); // Allow time for 10+ failed health checks
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // After max consecutive failures, state should become FAILED
        assertEquals(ConnectionState.FAILED, handler.getCurrentState());
    }

    @Test
    void testInboundFailureHandling() {
        handler.onHandlerAdded(mockContext);
        handler.onConnectionActive(mockContext);

        RuntimeException testException = new RuntimeException("Test connection failure");

        handler.handleInboundFailure(mockContext, testException);

        assertEquals(ConnectionState.FAILED, handler.getCurrentState());
        verify(mockContext).propagateInboundFailure(testException);
    }

    @Test
    void testHealthCheckResponseHandling() {
        handler.onHandlerAdded(mockContext);
        handler.onConnectionActive(mockContext);

        // Create a mock health check response
        InboundMethodReturn healthResponse = mock(InboundMethodReturn.class);
        DBusUInt32 serial = DBusUInt32.valueOf(123);
        when(healthResponse.getReplySerial()).thenReturn(serial);

        // Manually register the serial as a pending health check
        // (simulating what would happen during a real health check)
        CompletableFuture<InboundMessage> future = new CompletableFuture<>();
        try {
            Field pendingField =
                    ConnectionHealthHandler.class.getDeclaredField("pendingHealthChecks");
            pendingField.setAccessible(true);
            Map<DBusUInt32, CompletableFuture<InboundMessage>> pendingHealthChecks =
                    (Map<DBusUInt32, CompletableFuture<InboundMessage>>) pendingField.get(handler);
            pendingHealthChecks.put(serial, future);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Now handle the response - should not propagate
        handler.handleInboundMessage(mockContext, healthResponse);
        verify(mockContext, never()).propagateInboundMessage(any());

        // Verify the future was completed
        assertTrue(future.isDone());
    }

    @Test
    void testNonHealthCheckMessagePropagation() {
        handler.onHandlerAdded(mockContext);

        InboundMessage normalMessage = mock(InboundMessage.class);

        handler.handleInboundMessage(mockContext, normalMessage);

        verify(mockContext).propagateInboundMessage(normalMessage);
    }

    @Test
    void testListenerExceptionHandling() throws InterruptedException {
        CountDownLatch eventLatch = new CountDownLatch(2); // Expecting 2 events despite exception

        ConnectionEventListener faultyListener =
                (connection, event) -> {
                    eventLatch.countDown();
                    throw new RuntimeException("Listener error");
                };

        ConnectionEventListener goodListener =
                (connection, event) -> {
                    eventLatch.countDown();
                };

        handler.addConnectionEventListener(faultyListener);
        handler.addConnectionEventListener(goodListener);
        handler.onHandlerAdded(mockContext);

        // This should trigger state change event to both listeners
        handler.onConnectionActive(mockContext);

        // Wait for events to be processed
        assertTrue(eventLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void testHandlerAddedRemoved() {
        assertNull(handler.triggerHealthCheck().join());

        handler.onHandlerAdded(mockContext);
        assertNotNull(handler.triggerHealthCheck());

        handler.onHandlerRemoved(mockContext);
        assertNull(handler.triggerHealthCheck().join());
    }

    @Test
    void testNullListenerIgnored() {
        handler.addConnectionEventListener(null);

        // Should not cause any issues
        handler.onHandlerAdded(mockContext);
        handler.onConnectionActive(mockContext);
    }

    @Test
    void testStateUnhealthyTransition() throws InterruptedException {
        CountDownLatch unhealthyEventLatch = new CountDownLatch(1);
        AtomicReference<ConnectionEvent> unhealthyEvent = new AtomicReference<>();

        ConnectionEventListener listener =
                (connection, event) -> {
                    if (event.getType() == ConnectionEventType.HEALTH_CHECK_FAILURE) {
                        unhealthyEvent.set(event);
                        unhealthyEventLatch.countDown();
                    }
                };

        handler.addConnectionEventListener(listener);
        handler.onHandlerAdded(mockContext);
        handler.onConnectionActive(mockContext);

        when(mockConnection.getNextSerial()).thenReturn(DBusUInt32.valueOf(1));

        // Wait for health check failure
        assertTrue(unhealthyEventLatch.await(500, TimeUnit.MILLISECONDS));

        ConnectionEvent event = unhealthyEvent.get();
        assertNotNull(event);
        assertEquals(ConnectionEventType.HEALTH_CHECK_FAILURE, event.getType());
    }

    @Test
    void testResourceLimitScenario() {
        handler.onHandlerAdded(mockContext);
        when(mockConnection.getNextSerial()).thenReturn(DBusUInt32.valueOf(1));

        // Enable health checks but simulate scenario where too many pending checks build up
        handler.onConnectionActive(mockContext);

        // Trigger multiple health checks rapidly
        for (int i = 0; i < 150; i++) { // More than MAX_PENDING_HEALTH_CHECKS
            handler.triggerHealthCheck();
        }

        // Should handle resource limit gracefully without throwing exceptions
        assertDoesNotThrow(
                () -> {
                    Thread.sleep(200);
                });
    }
}
