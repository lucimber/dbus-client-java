/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConnectionEventTest {

    @Test
    void testBuilderWithAllFields() {
        Instant timestamp = Instant.now();
        RuntimeException cause = new RuntimeException("Test exception");

        ConnectionEvent event =
                ConnectionEvent.builder(ConnectionEventType.STATE_CHANGED)
                        .withOldState(ConnectionState.CONNECTED)
                        .withNewState(ConnectionState.DISCONNECTED)
                        .withTimestamp(timestamp)
                        .withMessage("Test message")
                        .withCause(cause)
                        .build();

        assertEquals(ConnectionEventType.STATE_CHANGED, event.getType());
        assertEquals(Optional.of(ConnectionState.CONNECTED), event.getOldState());
        assertEquals(Optional.of(ConnectionState.DISCONNECTED), event.getNewState());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals(Optional.of("Test message"), event.getMessage());
        assertEquals(Optional.of(cause), event.getCause());
    }

    @Test
    void testBuilderWithMinimalFields() {
        ConnectionEvent event =
                ConnectionEvent.builder(ConnectionEventType.HEALTH_CHECK_SUCCESS).build();

        assertEquals(ConnectionEventType.HEALTH_CHECK_SUCCESS, event.getType());
        assertEquals(Optional.empty(), event.getOldState());
        assertEquals(Optional.empty(), event.getNewState());
        assertNotNull(event.getTimestamp());
        assertEquals(Optional.empty(), event.getMessage());
        assertEquals(Optional.empty(), event.getCause());
    }

    @Test
    void testBuilderWithNullType() {
        assertThrows(NullPointerException.class, () -> ConnectionEvent.builder(null).build());
    }

    @Test
    void testStateChangedFactory() {
        ConnectionEvent event =
                ConnectionEvent.stateChanged(
                        ConnectionState.DISCONNECTED, ConnectionState.CONNECTING);

        assertEquals(ConnectionEventType.STATE_CHANGED, event.getType());
        assertEquals(Optional.of(ConnectionState.DISCONNECTED), event.getOldState());
        assertEquals(Optional.of(ConnectionState.CONNECTING), event.getNewState());
        assertEquals(
                Optional.of("Connection state changed from DISCONNECTED to CONNECTING"),
                event.getMessage());
    }

    @Test
    void testHealthCheckSuccessFactory() {
        ConnectionEvent event = ConnectionEvent.healthCheckSuccess();

        assertEquals(ConnectionEventType.HEALTH_CHECK_SUCCESS, event.getType());
        assertEquals(Optional.empty(), event.getOldState());
        assertEquals(Optional.empty(), event.getNewState());
        assertEquals(Optional.of("Health check succeeded"), event.getMessage());
        assertEquals(Optional.empty(), event.getCause());
    }

    @Test
    void testHealthCheckFailureFactory() {
        RuntimeException cause = new RuntimeException("Connection timeout");
        ConnectionEvent event = ConnectionEvent.healthCheckFailure(cause);

        assertEquals(ConnectionEventType.HEALTH_CHECK_FAILURE, event.getType());
        assertEquals(Optional.empty(), event.getOldState());
        assertEquals(Optional.empty(), event.getNewState());
        assertEquals(Optional.of("Health check failed: Connection timeout"), event.getMessage());
        assertEquals(Optional.of(cause), event.getCause());
    }

    @Test
    void testReconnectionAttemptFactory() {
        ConnectionEvent event = ConnectionEvent.reconnectionAttempt(3);

        assertEquals(ConnectionEventType.RECONNECTION_ATTEMPT, event.getType());
        assertEquals(Optional.empty(), event.getOldState());
        assertEquals(Optional.empty(), event.getNewState());
        assertEquals(Optional.of("Reconnection attempt #3"), event.getMessage());
        assertEquals(Optional.empty(), event.getCause());
    }

    @Test
    void testToStringWithAllFields() {
        Instant timestamp = Instant.parse("2024-01-01T12:00:00Z");
        RuntimeException cause = new RuntimeException("Test");

        ConnectionEvent event =
                ConnectionEvent.builder(ConnectionEventType.STATE_CHANGED)
                        .withOldState(ConnectionState.CONNECTED)
                        .withNewState(ConnectionState.FAILED)
                        .withTimestamp(timestamp)
                        .withMessage("Connection failed")
                        .withCause(cause)
                        .build();

        String str = event.toString();
        assertTrue(str.contains("ConnectionEvent{"));
        assertTrue(str.contains("type=STATE_CHANGED"));
        assertTrue(str.contains("timestamp=2024-01-01T12:00:00Z"));
        assertTrue(str.contains("state=CONNECTED->FAILED"));
        assertTrue(str.contains("message='Connection failed'"));
        assertTrue(str.contains("cause=RuntimeException"));
        assertTrue(str.endsWith("}"));
    }

    @Test
    void testToStringWithMinimalFields() {
        ConnectionEvent event =
                ConnectionEvent.builder(ConnectionEventType.HEALTH_CHECK_SUCCESS).build();

        String str = event.toString();
        assertTrue(str.contains("ConnectionEvent{"));
        assertTrue(str.contains("type=HEALTH_CHECK_SUCCESS"));
        assertTrue(str.contains("timestamp="));
        assertFalse(str.contains("state="));
        assertFalse(str.contains("message="));
        assertFalse(str.contains("cause="));
        assertTrue(str.endsWith("}"));
    }

    @Test
    void testToStringWithOnlyNewState() {
        ConnectionEvent event =
                ConnectionEvent.builder(ConnectionEventType.STATE_CHANGED)
                        .withNewState(ConnectionState.CONNECTED)
                        .build();

        String str = event.toString();
        assertTrue(str.contains("state=null->CONNECTED"));
    }

    @Test
    void testToStringWithOnlyOldState() {
        ConnectionEvent event =
                ConnectionEvent.builder(ConnectionEventType.STATE_CHANGED)
                        .withOldState(ConnectionState.DISCONNECTED)
                        .build();

        String str = event.toString();
        assertTrue(str.contains("state=DISCONNECTED->null"));
    }

    @Test
    void testTimestampDefaultsToNow() {
        Instant before = Instant.now();
        ConnectionEvent event =
                ConnectionEvent.builder(ConnectionEventType.HEALTH_CHECK_SUCCESS).build();
        Instant after = Instant.now();

        assertNotNull(event.getTimestamp());
        assertFalse(event.getTimestamp().isBefore(before));
        assertFalse(event.getTimestamp().isAfter(after));
    }

    @Test
    void testBuilderChaining() {
        ConnectionEvent.Builder builder =
                ConnectionEvent.builder(ConnectionEventType.STATE_CHANGED);

        assertSame(builder, builder.withOldState(ConnectionState.CONNECTED));
        assertSame(builder, builder.withNewState(ConnectionState.DISCONNECTED));
        assertSame(builder, builder.withTimestamp(Instant.now()));
        assertSame(builder, builder.withMessage("test"));
        assertSame(builder, builder.withCause(new RuntimeException()));
    }

    @Test
    void testConnectionEventTypeUsage() {
        // Test all event types are properly handled
        for (ConnectionEventType type : ConnectionEventType.values()) {
            ConnectionEvent event = ConnectionEvent.builder(type).build();
            assertEquals(type, event.getType());
        }
    }

    @Test
    void testNullCauseMessage() {
        ConnectionEvent event = ConnectionEvent.healthCheckFailure(new RuntimeException());
        assertEquals(Optional.of("Health check failed: null"), event.getMessage());
    }
}
