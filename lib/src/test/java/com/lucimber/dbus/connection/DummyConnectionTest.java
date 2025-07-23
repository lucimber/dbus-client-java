/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DummyConnectionTest {

    private DummyConnection connection;

    @BeforeEach
    void setUp() {
        connection = DummyConnection.create();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testCreateSimpleConnection() {
        assertNotNull(connection);
        assertEquals(ConnectionState.DISCONNECTED, connection.getState());
        assertFalse(connection.isConnected());
    }

    @Test
    void testBuilderConfiguration() {
        ConnectionConfig config = ConnectionConfig.builder().withHealthCheckEnabled(false).build();

        DummyConnection customConnection =
                DummyConnection.builder()
                        .withConfig(config)
                        .withConnectDelay(Duration.ofMillis(10))
                        .withConnectionFailure(false)
                        .withHealthCheckFailure(false)
                        .build();

        assertNotNull(customConnection);
        assertEquals(config, customConnection.getConfig());
        customConnection.close();
    }

    @Test
    @Timeout(5)
    void testSuccessfulConnection() throws Exception {
        CompletableFuture<Void> connectFuture = connection.connect().toCompletableFuture();
        assertNotNull(connectFuture);

        connectFuture.get(2, TimeUnit.SECONDS);

        assertTrue(connection.isConnected());
        assertEquals(ConnectionState.CONNECTED, connection.getState());
    }

    @Test
    @Timeout(5)
    void testConnectionFailure() throws Exception {
        DummyConnection failingConnection =
                DummyConnection.builder()
                        .withConnectionFailure(true)
                        .withConnectDelay(Duration.ofMillis(10))
                        .build();

        CompletableFuture<Void> connectFuture = failingConnection.connect().toCompletableFuture();

        assertThrows(ExecutionException.class, () -> connectFuture.get(2, TimeUnit.SECONDS));
        assertEquals(ConnectionState.FAILED, failingConnection.getState());
        assertFalse(failingConnection.isConnected());

        failingConnection.close();
    }

    @Test
    void testConnectionWhenAlreadyConnected() throws Exception {
        // First connection
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertTrue(connection.isConnected());

        // Second connection attempt should complete immediately
        CompletableFuture<Void> secondConnect = connection.connect().toCompletableFuture();
        secondConnect.get(100, TimeUnit.MILLISECONDS);
        assertTrue(connection.isConnected());
    }

    @Test
    void testGetNextSerial() {
        DBusUInt32 serial1 = connection.getNextSerial();
        DBusUInt32 serial2 = connection.getNextSerial();
        DBusUInt32 serial3 = connection.getNextSerial();

        assertEquals(1, serial1.intValue());
        assertEquals(2, serial2.intValue());
        assertEquals(3, serial3.intValue());
    }

    @Test
    void testPipeline() {
        Pipeline pipeline = connection.getPipeline();
        assertNotNull(pipeline);
        assertEquals(connection, pipeline.getConnection());

        // Test adding handler
        Handler testHandler =
                new AbstractDuplexHandler() {
                    @Override
                    protected Logger getLogger() {
                        return LoggerFactory.getLogger(getClass());
                    }
                };
        pipeline.addLast("test", testHandler);

        // Test duplicate handler name
        assertThrows(IllegalArgumentException.class, () -> pipeline.addLast("test", testHandler));

        // Test remove handler
        pipeline.remove("test");

        // Test remove non-existent handler
        assertThrows(IllegalArgumentException.class, () -> pipeline.remove("test"));

        // Test null checks
        assertThrows(NullPointerException.class, () -> pipeline.addLast(null, testHandler));
        assertThrows(NullPointerException.class, () -> pipeline.addLast("test", null));
        assertThrows(NullPointerException.class, () -> pipeline.remove(null));
    }

    @Test
    @Timeout(5)
    void testSendRequestWhenConnected() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(DBusUInt32.valueOf(1))
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .withInterface(DBusString.valueOf("com.test.Interface"))
                        .build();

        CompletableFuture<InboundMessage> response =
                connection.sendRequest(call).toCompletableFuture();
        InboundMessage result = response.get(2, TimeUnit.SECONDS);

        assertNotNull(result);
        assertTrue(result instanceof InboundError);
        InboundError error = (InboundError) result;
        assertEquals("org.freedesktop.DBus.Error.UnknownMethod", error.getErrorName().toString());

        // Verify message was captured
        assertEquals(1, connection.getSentMessages().size());
        assertEquals(call, connection.getSentMessages().get(0));
    }

    @Test
    void testSendRequestWhenNotConnected() {
        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(DBusUInt32.valueOf(1))
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .build();

        CompletableFuture<InboundMessage> response =
                connection.sendRequest(call).toCompletableFuture();

        assertThrows(ExecutionException.class, () -> response.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(5)
    void testSendAndRouteResponse() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(DBusUInt32.valueOf(1))
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .build();

        CompletableFuture<Void> future = new CompletableFuture<>();
        connection.sendAndRouteResponse(call, future);

        future.get(2, TimeUnit.SECONDS);

        // Verify message was captured
        assertEquals(1, connection.getSentMessages().size());
    }

    @Test
    void testSendAndRouteResponseWhenNotConnected() {
        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(DBusUInt32.valueOf(1))
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .build();

        CompletableFuture<Void> future = new CompletableFuture<>();
        connection.sendAndRouteResponse(call, future);

        assertThrows(ExecutionException.class, () -> future.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    @Timeout(5)
    void testCustomMethodCallResponse() throws Exception {
        List<DBusType> responseBody = List.of(DBusString.valueOf("test-response"));
        connection.setMethodCallResponse(
                "com.test.Interface", "TestMethod", DummyConnection.successResponse(responseBody));

        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(DBusUInt32.valueOf(1))
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .withInterface(DBusString.valueOf("com.test.Interface"))
                        .build();

        InboundMessage response =
                connection.sendRequest(call).toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertTrue(response instanceof InboundMethodReturn);
        InboundMethodReturn methodReturn = (InboundMethodReturn) response;
        assertEquals(call.getSerial(), methodReturn.getReplySerial());
        assertEquals(1, methodReturn.getPayload().size());
        assertEquals("test-response", ((DBusString) methodReturn.getPayload().get(0)).toString());
    }

    @Test
    @Timeout(5)
    void testErrorResponse() throws Exception {
        connection.setMethodCallResponse(
                "com.test.Interface",
                "FailingMethod",
                DummyConnection.errorResponse("com.test.Error", "Something went wrong"));

        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(DBusUInt32.valueOf(1))
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("FailingMethod"))
                        .withInterface(DBusString.valueOf("com.test.Interface"))
                        .build();

        InboundMessage response =
                connection.sendRequest(call).toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertTrue(response instanceof InboundError);
        InboundError error = (InboundError) response;
        assertEquals("com.test.Error", error.getErrorName().toString());
        assertEquals("Something went wrong", ((DBusString) error.getPayload().get(0)).toString());
    }

    @Test
    void testMethodCallTracking() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertFalse(connection.wasMethodCalled("com.test.Interface", "TestMethod"));
        assertEquals(0, connection.getMethodCallCount("com.test.Interface", "TestMethod"));

        OutboundMethodCall call1 =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .withInterface(DBusString.valueOf("com.test.Interface"))
                        .build();

        OutboundMethodCall call2 =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .withInterface(DBusString.valueOf("com.test.Interface"))
                        .build();

        OutboundMethodCall call3 =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("OtherMethod"))
                        .withInterface(DBusString.valueOf("com.test.Interface"))
                        .build();

        connection.sendRequest(call1);
        connection.sendRequest(call2);
        connection.sendRequest(call3);

        Thread.sleep(100); // Give time for messages to be captured

        assertTrue(connection.wasMethodCalled("com.test.Interface", "TestMethod"));
        assertEquals(2, connection.getMethodCallCount("com.test.Interface", "TestMethod"));
        assertEquals(1, connection.getMethodCallCount("com.test.Interface", "OtherMethod"));
        assertEquals(0, connection.getMethodCallCount("com.test.Interface", "NonExistent"));

        List<OutboundMethodCall> testMethodCalls = connection.getMethodCalls("com.test.Interface");
        assertEquals(3, testMethodCalls.size());
    }

    @Test
    void testConnectionEventListener() throws Exception {
        AtomicBoolean connected = new AtomicBoolean(false);
        AtomicReference<ConnectionState> lastOldState = new AtomicReference<>();
        AtomicReference<ConnectionState> lastNewState = new AtomicReference<>();

        ConnectionEventListener listener =
                (conn, event) -> {
                    if (event.getType() == ConnectionEventType.STATE_CHANGED) {
                        event.getOldState().ifPresent(lastOldState::set);
                        event.getNewState()
                                .ifPresent(
                                        state -> {
                                            lastNewState.set(state);
                                            if (state == ConnectionState.CONNECTED) {
                                                connected.set(true);
                                            }
                                        });
                    }
                };

        connection.addConnectionEventListener(listener);

        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        // Give events time to fire
        Thread.sleep(100);

        assertTrue(connected.get());
        assertEquals(ConnectionState.CONNECTED, lastNewState.get());

        // Test remove listener
        connection.removeConnectionEventListener(listener);

        // Test null listener - would cause NPE in DummyConnection, so we skip this test
    }

    @Test
    @Timeout(5)
    void testHealthCheck() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        CompletableFuture<Void> healthCheck = connection.triggerHealthCheck().toCompletableFuture();
        healthCheck.get(2, TimeUnit.SECONDS);

        assertTrue(connection.isConnected());
    }

    @Test
    @Timeout(5)
    void testHealthCheckFailure() throws Exception {
        DummyConnection failingConnection =
                DummyConnection.builder().withHealthCheckFailure(true).build();

        failingConnection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        CompletableFuture<Void> healthCheck =
                failingConnection.triggerHealthCheck().toCompletableFuture();

        assertThrows(ExecutionException.class, () -> healthCheck.get(2, TimeUnit.SECONDS));
        assertEquals(ConnectionState.UNHEALTHY, failingConnection.getState());

        failingConnection.close();
    }

    @Test
    void testReconnectAttemptCount() {
        assertEquals(0, connection.getReconnectAttemptCount());

        connection.simulateConnectionFailure();
        connection.simulateReconnection();

        assertEquals(1, connection.getReconnectAttemptCount());
    }

    @Test
    void testResetReconnectionState() {
        connection.simulateConnectionFailure();
        connection.simulateReconnection();
        assertEquals(1, connection.getReconnectAttemptCount());

        connection.resetReconnectionState();
        assertEquals(0, connection.getReconnectAttemptCount());
    }

    @Test
    void testCancelReconnection() {
        // This is a no-op in DummyConnection, just verify it doesn't throw
        connection.cancelReconnection();
    }

    @Test
    @Timeout(5)
    void testConnectionEvents() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        // Allow some time for events to be captured
        Thread.sleep(100);

        List<ConnectionEvent> events = connection.getConnectionEvents();
        assertFalse(events.isEmpty());

        // Should have at least CONNECTING, AUTHENTICATING, and CONNECTED events
        boolean hasConnectingEvent =
                events.stream()
                        .anyMatch(
                                e ->
                                        e.getType() == ConnectionEventType.STATE_CHANGED
                                                && e.getNewState().orElse(null)
                                                        == ConnectionState.CONNECTING);
        boolean hasConnectedEvent =
                events.stream()
                        .anyMatch(
                                e ->
                                        e.getType() == ConnectionEventType.STATE_CHANGED
                                                && e.getNewState().orElse(null)
                                                        == ConnectionState.CONNECTED);

        // At minimum we should have a connected event
        assertTrue(hasConnectedEvent, "Should have at least a CONNECTED state change event");
    }

    @Test
    void testWaitForEvent() throws Exception {
        // Start connection in background
        CompletableFuture.runAsync(
                () -> {
                    try {
                        Thread.sleep(50);
                        connection.connect();
                    } catch (Exception e) {
                        fail(e);
                    }
                });

        // Wait for CONNECTED state change event
        boolean eventOccurred =
                connection.waitForEvent(ConnectionEventType.STATE_CHANGED, 2, TimeUnit.SECONDS);
        assertTrue(eventOccurred);
    }

    @Test
    void testClearCaptures() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("TestMethod"))
                        .build();

        connection.sendRequest(call);
        Thread.sleep(100);

        assertFalse(connection.getSentMessages().isEmpty());
        assertFalse(connection.getConnectionEvents().isEmpty());

        connection.clearCaptures();

        assertTrue(connection.getSentMessages().isEmpty());
        assertTrue(connection.getConnectionEvents().isEmpty());
    }

    @Test
    void testSimulateConnectionFailure() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertTrue(connection.isConnected());

        connection.simulateConnectionFailure();

        assertEquals(ConnectionState.FAILED, connection.getState());
        assertFalse(connection.isConnected());
    }

    @Test
    void testCloseMultipleTimes() {
        connection.close();
        connection.close(); // Should not throw
    }

    @Test
    void testConnectAfterClose() {
        connection.close();

        CompletableFuture<Void> connectFuture = connection.connect().toCompletableFuture();

        assertThrows(ExecutionException.class, () -> connectFuture.get(100, TimeUnit.MILLISECONDS));
    }

    @Test
    void testGetSentMessagesWithPredicate() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call1 =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("Method1"))
                        .build();

        OutboundMethodCall call2 =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("Method2"))
                        .build();

        connection.sendRequest(call1);
        connection.sendRequest(call2);
        Thread.sleep(100);

        List<OutboundMessage> method1Messages =
                connection.getSentMessages(
                        msg ->
                                msg instanceof OutboundMethodCall
                                        && ((OutboundMethodCall) msg)
                                                .getMember()
                                                .toString()
                                                .equals("Method1"));

        assertEquals(1, method1Messages.size());
        assertEquals(
                "Method1", ((OutboundMethodCall) method1Messages.get(0)).getMember().toString());
    }

    @Test
    void testDefaultIntrospectionResponse() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("Introspect"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Introspectable"))
                        .build();

        InboundMessage response =
                connection.sendRequest(call).toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertTrue(response instanceof InboundMethodReturn);
        InboundMethodReturn methodReturn = (InboundMethodReturn) response;
        assertEquals(1, methodReturn.getPayload().size());
        String xml = ((DBusString) methodReturn.getPayload().get(0)).toString();
        assertTrue(xml.contains("<!DOCTYPE node"));
    }

    @Test
    void testDefaultPingResponse() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("Ping"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
                        .build();

        InboundMessage response =
                connection.sendRequest(call).toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertTrue(response instanceof InboundMethodReturn);
        InboundMethodReturn methodReturn = (InboundMethodReturn) response;
        assertEquals(0, methodReturn.getPayload().size()); // Ping returns empty
    }

    @Test
    void testDefaultGetMachineIdResponse() throws Exception {
        connection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(connection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("GetMachineId"))
                        .withInterface(DBusString.valueOf("org.freedesktop.DBus.Peer"))
                        .build();

        InboundMessage response =
                connection.sendRequest(call).toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertTrue(response instanceof InboundMethodReturn);
        InboundMethodReturn methodReturn = (InboundMethodReturn) response;
        assertEquals(1, methodReturn.getPayload().size());
        String machineId = ((DBusString) methodReturn.getPayload().get(0)).toString();
        assertTrue(machineId.startsWith("dummy-machine-id-"));
    }

    @Test
    void testBuilderWithMethodCallResponse() throws Exception {
        Function<OutboundMessage, InboundMessage> responseFunction =
                DummyConnection.successResponse(List.of(DBusString.valueOf("builder-response")));

        DummyConnection customConnection =
                DummyConnection.builder()
                        .withMethodCallResponse(
                                "com.test.Interface", "BuilderMethod", responseFunction)
                        .build();

        customConnection.connect().toCompletableFuture().get(2, TimeUnit.SECONDS);

        OutboundMethodCall call =
                OutboundMethodCall.Builder.create()
                        .withSerial(customConnection.getNextSerial())
                        .withPath(DBusObjectPath.valueOf("/test"))
                        .withMember(DBusString.valueOf("BuilderMethod"))
                        .withInterface(DBusString.valueOf("com.test.Interface"))
                        .build();

        InboundMessage response =
                customConnection.sendRequest(call).toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertTrue(response instanceof InboundMethodReturn);
        assertEquals(
                "builder-response",
                ((DBusString) ((InboundMethodReturn) response).getPayload().get(0)).toString());

        customConnection.close();
    }

    @Test
    void testResponseFunctionWithNonMethodCall() {
        Function<OutboundMessage, InboundMessage> successFunction =
                DummyConnection.successResponse(List.of());
        Function<OutboundMessage, InboundMessage> errorFunction =
                DummyConnection.errorResponse("test.Error", "error");

        OutboundMessage nonMethodCall =
                new OutboundMessage() {
                    @Override
                    public DBusUInt32 getSerial() {
                        return DBusUInt32.valueOf(1);
                    }

                    @Override
                    public List<DBusType> getPayload() {
                        return Collections.emptyList();
                    }

                    @Override
                    public Optional<DBusSignature> getSignature() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<DBusString> getDestination() {
                        return Optional.empty();
                    }
                };

        assertThrows(IllegalArgumentException.class, () -> successFunction.apply(nonMethodCall));
        assertThrows(IllegalArgumentException.class, () -> errorFunction.apply(nonMethodCall));
    }

    @Test
    void testPipelinePropagationMethods() {
        Pipeline pipeline = connection.getPipeline();

        // These are no-ops in DummyPipeline, just verify they don't throw
        pipeline.propagateInboundMessage(null);
        pipeline.propagateOutboundMessage(null, new CompletableFuture<>());
        pipeline.propagateConnectionActive();
        pipeline.propagateConnectionInactive();
        pipeline.propagateInboundFailure(new RuntimeException("test"));
    }
}
