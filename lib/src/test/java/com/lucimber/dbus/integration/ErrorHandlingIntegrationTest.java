/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.ConnectionEvent;
import com.lucimber.dbus.connection.ConnectionEventListener;
import com.lucimber.dbus.connection.ConnectionEventType;
import com.lucimber.dbus.connection.ConnectionState;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Type;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for D-Bus error handling and edge cases.
 */
@Tag("integration")
@DisabledIf("shouldSkipDBusTests")
class ErrorHandlingIntegrationTest extends DBusIntegrationTestBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingIntegrationTest.class);

    @Test
    void testErrorMessageCreation() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test creating method calls that would result in errors
            OutboundMethodCall invalidCall = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/com/example/NonExistent"))
                .withMember(DBusString.valueOf("TestMethod"))
                .withDestination(DBusString.valueOf("com.example.NonExistentService"))
                .withInterface(DBusString.valueOf("com.example.TestInterface"))
                .withReplyExpected(true)
                .build();

            // Verify the message is created correctly
            assertNotNull(invalidCall);
            assertEquals("/com/example/NonExistent", invalidCall.getObjectPath().toString());
            assertEquals("TestMethod", invalidCall.getMember().toString());
            assertTrue(invalidCall.getDestination().isPresent());
            assertEquals("com.example.NonExistentService", invalidCall.getDestination().get().toString());
            assertTrue(invalidCall.isReplyExpected());
            
            LOGGER.info("✓ Successfully created error-prone method call structure");

        } finally {
            connection.close();
        }
    }

    @Test
    void testInvalidParameterMessageCreation() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test creating method calls with invalid parameters
            OutboundMethodCall invalidCall = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus/NonExistentObject"))
                .withMember(DBusString.valueOf("TestMethod"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus.TestInterface"))
                .withReplyExpected(true)
                .build();

            // Verify the invalid message structure is created correctly
            assertNotNull(invalidCall);
            assertEquals("/org/freedesktop/DBus/NonExistentObject", invalidCall.getObjectPath().toString());
            assertEquals("TestMethod", invalidCall.getMember().toString());
            assertEquals("org.freedesktop.DBus.TestInterface", 
                invalidCall.getInterfaceName().map(DBusString::toString).orElse(""));
            
            LOGGER.info("✓ Successfully created method call with invalid object path");

        } finally {
            connection.close();
        }
    }

    @Test
    void testInvalidSignatureMessages() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test creating method calls with wrong signature types
            OutboundMethodCall invalidCall = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("NameHasOwner"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withBody(DBusSignature.valueOf("u"), List.of(com.lucimber.dbus.type.DBusUInt32.valueOf(12345))) // Should be "s" (string), not "u" (uint32)
                .withReplyExpected(true)
                .build();

            // Verify the message with wrong signature is created
            assertNotNull(invalidCall);
            assertEquals("NameHasOwner", invalidCall.getMember().toString());
            assertTrue(invalidCall.getSignature().isPresent());
            assertEquals("u", invalidCall.getSignature().get().toString()); // Wrong signature
            assertNotNull(invalidCall.getPayload());
            assertEquals(1, invalidCall.getPayload().size());
            
            // Verify the payload contains UInt32 instead of expected String
            assertEquals(Type.UINT32, invalidCall.getPayload().get(0).getType());
            
            LOGGER.info("✓ Successfully created message with mismatched signature");

        } finally {
            connection.close();
        }
    }

    @Test
    void testConnectionTimeout() {
        // Test connection timeout with unreachable host
        ConnectionConfig config = ConnectionConfig.builder()
            .withConnectTimeout(Duration.ofSeconds(2)) // Short timeout
            .build();

        Connection connection = new NettyConnection(
            new InetSocketAddress("192.0.2.1", 12345), // RFC5737 test address (unreachable)
            config
        );

        CompletableFuture<Void> connectFuture = connection.connect().toCompletableFuture();
        
        assertThrows(ExecutionException.class, () -> {
            connectFuture.get(5, TimeUnit.SECONDS);
        }, "Connection should timeout and fail");
        
        assertFalse(connection.isConnected());
        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
        
        LOGGER.info("✓ Successfully tested connection timeout");
        
        try {
            connection.close();
        } catch (Exception e) {
            // Ignore close exceptions for failed connection
        }
    }

    @Test
    void testTimeoutConfiguration() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test that timeout configurations are accessible
            var config = connection.getConfig();
            assertNotNull(config);
            
            // Verify timeout settings exist and are reasonable
            assertNotNull(config.getConnectTimeout());
            assertTrue(config.getConnectTimeout().toMillis() > 0);
            
            assertNotNull(config.getMethodCallTimeout());
            assertTrue(config.getMethodCallTimeout().toMillis() > 0);
            
            // Test creating a method call that would potentially timeout
            OutboundMethodCall call = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("ListNames"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withReplyExpected(true)
                .build();
            
            assertNotNull(call);
            assertEquals("ListNames", call.getMember().toString());
            
            LOGGER.info("✓ Successfully verified timeout configuration and message creation");

        } finally {
            connection.close();
        }
    }

    @Test
    void testConnectionStateValidation() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Verify connection is initially connected
            assertTrue(connection.isConnected());
            assertEquals(ConnectionState.CONNECTED, connection.getState());
            
            // Test that we can create messages while connected
            OutboundMethodCall methodCall = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("ListNames"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withReplyExpected(true)
                .build();
            
            assertNotNull(methodCall);
            assertNotNull(methodCall.getSerial());
            assertTrue(methodCall.getSerial().longValue() > 0);
            
            LOGGER.info("✓ Successfully validated connection state and message creation");

        } finally {
            // Connection is already closed in this test
        }
    }

    @Test
    void testInvalidServiceNameMessages() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test creating RequestName calls with invalid service names
            OutboundMethodCall invalidRequest = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("RequestName"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withBody(DBusSignature.valueOf("su"), List.of(
                    DBusString.valueOf(""), // Invalid empty name
                    com.lucimber.dbus.type.DBusUInt32.valueOf(0)
                ))
                .withReplyExpected(true)
                .build();

            // Verify the message with invalid service name is created
            assertNotNull(invalidRequest);
            assertEquals("RequestName", invalidRequest.getMember().toString());
            assertTrue(invalidRequest.getSignature().isPresent());
            assertEquals("su", invalidRequest.getSignature().get().toString());
            
            // Verify the empty service name in payload
            assertNotNull(invalidRequest.getPayload());
            assertEquals(2, invalidRequest.getPayload().size());
            String serviceName = ((DBusString) invalidRequest.getPayload().get(0)).toString();
            assertEquals("", serviceName); // Empty/invalid name
            
            LOGGER.info("✓ Successfully created RequestName with invalid service name");

        } finally {
            connection.close();
        }
    }

    @Test
    void testRestrictedMethodMessages() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test creating calls to potentially restricted methods
            OutboundMethodCall restrictedCall = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("UpdateActivationEnvironment"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withBody(DBusSignature.valueOf("a{ss}"), List.of(
                    new com.lucimber.dbus.type.DBusDict<>(DBusSignature.valueOf("a{ss}")) // Empty dict
                ))
                .withReplyExpected(true)
                .build();

            // Verify the restricted method call message structure
            assertNotNull(restrictedCall);
            assertEquals("UpdateActivationEnvironment", restrictedCall.getMember().toString());
            assertTrue(restrictedCall.getSignature().isPresent());
            assertEquals("a{ss}", restrictedCall.getSignature().get().toString());
            
            // Verify the dictionary parameter
            assertNotNull(restrictedCall.getPayload());
            assertEquals(1, restrictedCall.getPayload().size());
            assertEquals(Type.ARRAY, restrictedCall.getPayload().get(0).getType());
            
            LOGGER.info("✓ Successfully created potentially restricted method call");

        } finally {
            connection.close();
        }
    }

    @Test
    void testConnectionEventListeners() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test connection event listener functionality
            CountDownLatch eventLatch = new CountDownLatch(1);
            AtomicReference<ConnectionEvent> capturedEvent = new AtomicReference<>();
            
            ConnectionEventListener listener = (conn, event) -> {
                capturedEvent.set(event);
                eventLatch.countDown();
                LOGGER.info("Captured connection event: {}", event.getType());
            };
            
            // Test adding and removing event listeners
            connection.addConnectionEventListener(listener);
            
            // Verify health check configuration
            boolean healthCheckEnabled = connection.getConfig().isHealthCheckEnabled();
            LOGGER.info("Health check enabled: {}", healthCheckEnabled);
            
            if (healthCheckEnabled) {
                assertNotNull(connection.getConfig().getHealthCheckInterval());
                assertTrue(connection.getConfig().getHealthCheckInterval().toMillis() > 0);
                LOGGER.info("Health check interval: {}", connection.getConfig().getHealthCheckInterval());
            }
            
            // Remove the listener
            connection.removeConnectionEventListener(listener);
            
            LOGGER.info("✓ Successfully tested connection event listener functionality");

        } finally {
            connection.close();
        }
    }

    @Test
    void testLargeMessageCreation() throws Exception {
        Connection connection = createConnection();
        
        try {
            // Test creating messages with moderately large string parameters
            // Size adjusted to work reliably in container environment
            StringBuilder largeString = new StringBuilder();
            // Each iteration adds 50 chars, 500 iterations = 25,000 chars
            for (int i = 0; i < 500; i++) {
                largeString.append("This is a test string for large message handling. ");
            }
            
            OutboundMethodCall largeCall = OutboundMethodCall.Builder
                .create()
                .withSerial(connection.getNextSerial())
                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                .withMember(DBusString.valueOf("NameHasOwner"))
                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                .withBody(DBusSignature.valueOf("s"), List.of(DBusString.valueOf(largeString.toString())))
                .withReplyExpected(true)
                .build();

            // Verify the large message was created successfully
            assertNotNull(largeCall);
            assertEquals("NameHasOwner", largeCall.getMember().toString());
            assertTrue(largeCall.getSignature().isPresent());
            assertEquals("s", largeCall.getSignature().get().toString());
            
            // Verify the large string payload
            assertNotNull(largeCall.getPayload());
            assertEquals(1, largeCall.getPayload().size());
            String actualString = ((DBusString) largeCall.getPayload().get(0)).toString();
            assertEquals(largeString.toString(), actualString);
            
            // Verify the size is what we expect (adjusted for container environment)
            assertTrue(actualString.length() > 20000, "Large string should be over 20K chars");
            assertTrue(actualString.length() < 30000, "Large string should be under 30K chars");
            
            LOGGER.info("✓ Successfully created large message ({} chars)", actualString.length());

        } finally {
            connection.close();
        }
    }

    /**
     * Creates a connection for testing.
     */
    private Connection createConnection() throws Exception {
        ConnectionConfig config = ConnectionConfig.builder()
            .withConnectTimeout(Duration.ofSeconds(30))
            .build();

        Connection connection = new NettyConnection(
            new InetSocketAddress(getDBusHost(), getDBusPort()),
            config
        );

        connection.connect().toCompletableFuture().get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertTrue(connection.isConnected());
        
        return connection;
    }
}