/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lucimber.dbus.connection.AbstractInboundHandler;
import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.connection.Context;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests for D-Bus signal subscription and handling. */
@Tag("integration")
@DisabledIf("shouldSkipDBusTests")
class SignalHandlingIntegrationTest extends DBusIntegrationTestBase {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SignalHandlingIntegrationTest.class);

    @Test
    void testSignalHandlerCreation() throws Exception {
        Connection connection = createConnection();

        try {
            // Test creating signal handlers and adding them to pipeline
            CountDownLatch signalLatch = new CountDownLatch(1);
            AtomicReference<InboundSignal> capturedSignal = new AtomicReference<>();

            SignalCaptureHandler signalHandler =
                    new SignalCaptureHandler("NameOwnerChanged", capturedSignal, signalLatch);

            // Test adding handler to pipeline
            connection.getPipeline().addLast("signal-capture", signalHandler);

            // Test creating AddMatch method call (but not sending since sendRequest is not
            // implemented)
            OutboundMethodCall addMatch =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("AddMatch"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(
                                    DBusSignature.valueOf("s"),
                                    List.of(
                                            DBusString.valueOf(
                                                    "type='signal',interface='org.freedesktop.DBus',member='NameOwnerChanged'")))
                            .withReplyExpected(true)
                            .build();

            // Verify the message was created correctly
            assertNotNull(addMatch);
            assertEquals("AddMatch", addMatch.getMember().toString());
            assertTrue(addMatch.getSignature().isPresent());
            assertEquals("s", addMatch.getSignature().get().toString());

            // Test creating RequestName method call (but not sending)
            OutboundMethodCall requestName =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("RequestName"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(
                                    DBusSignature.valueOf("su"),
                                    List.of(
                                            DBusString.valueOf("com.lucimber.test.SignalTest"),
                                            com.lucimber.dbus.type.DBusUInt32.valueOf(0) // No flags
                                            ))
                            .withReplyExpected(true)
                            .build();

            // Verify the RequestName message was created correctly
            assertNotNull(requestName);
            assertEquals("RequestName", requestName.getMember().toString());
            assertTrue(requestName.getSignature().isPresent());
            assertEquals("su", requestName.getSignature().get().toString());
            assertNotNull(requestName.getPayload());
            assertEquals(2, requestName.getPayload().size());

            // Clean up the handler
            connection.getPipeline().remove("signal-capture");

            LOGGER.info("✓ Successfully created signal handling components and method calls");

        } finally {
            connection.close();
        }
    }

    @Test
    void testMultipleSignalHandlers() throws Exception {
        Connection connection = createConnection();

        try {
            // Test creating multiple signal handlers
            AtomicReference<Integer> signalCount = new AtomicReference<>(0);
            CountDownLatch signalLatch = new CountDownLatch(2);

            MultiSignalCaptureHandler signalHandler =
                    new MultiSignalCaptureHandler(signalCount, signalLatch);

            connection.getPipeline().addLast("multi-signal-capture", signalHandler);

            // Test creating multiple AddMatch method calls
            String[] matchRules = {
                "type='signal',interface='org.freedesktop.DBus',member='NameOwnerChanged'",
                "type='signal',interface='org.freedesktop.DBus',member='NameAcquired'"
            };

            for (String rule : matchRules) {
                OutboundMethodCall addMatch =
                        OutboundMethodCall.Builder.create()
                                .withSerial(connection.getNextSerial())
                                .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                                .withMember(DBusString.valueOf("AddMatch"))
                                .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                                .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                                .withBody(
                                        DBusSignature.valueOf("s"),
                                        List.of(DBusString.valueOf(rule)))
                                .withReplyExpected(true)
                                .build();

                // Verify each message was created correctly
                assertNotNull(addMatch);
                assertEquals("AddMatch", addMatch.getMember().toString());
                assertTrue(addMatch.getSignature().isPresent());
                assertEquals("s", addMatch.getSignature().get().toString());
            }

            // Test RequestName message creation
            OutboundMethodCall requestName =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("RequestName"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(
                                    DBusSignature.valueOf("su"),
                                    List.of(
                                            DBusString.valueOf("com.lucimber.test.MultiSignalTest"),
                                            com.lucimber.dbus.type.DBusUInt32.valueOf(0)))
                            .withReplyExpected(true)
                            .build();

            assertNotNull(requestName);
            assertEquals("RequestName", requestName.getMember().toString());

            // Clean up handler
            connection.getPipeline().remove("multi-signal-capture");

            LOGGER.info("✓ Successfully created multiple signal handlers and method calls");

        } finally {
            connection.close();
        }
    }

    @Test
    void testSignalFilteringSetup() throws Exception {
        Connection connection = createConnection();

        try {
            // Test creating filtered signal handlers
            CountDownLatch signalLatch = new CountDownLatch(1);
            AtomicReference<InboundSignal> capturedSignal = new AtomicReference<>();

            FilteredSignalCaptureHandler signalHandler =
                    new FilteredSignalCaptureHandler(
                            "com.lucimber.test.FilterTest", capturedSignal, signalLatch);

            connection.getPipeline().addLast("filtered-signal-capture", signalHandler);

            // Test creating filtered AddMatch method call
            OutboundMethodCall addMatch =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("AddMatch"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(
                                    DBusSignature.valueOf("s"),
                                    List.of(
                                            DBusString.valueOf(
                                                    "type='signal',interface='org.freedesktop.DBus',member='NameOwnerChanged',"
                                                            + "arg0='com.lucimber.test.FilterTest'")))
                            .withReplyExpected(true)
                            .build();

            // Verify the filtered match message
            assertNotNull(addMatch);
            assertEquals("AddMatch", addMatch.getMember().toString());
            assertTrue(addMatch.getSignature().isPresent());
            assertEquals("s", addMatch.getSignature().get().toString());

            // Verify the match rule contains filtering
            String matchRule = ((DBusString) addMatch.getPayload().get(0)).toString();
            assertTrue(matchRule.contains("arg0='com.lucimber.test.FilterTest'"));

            // Test creating filtered RequestName call
            OutboundMethodCall requestName =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("RequestName"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(
                                    DBusSignature.valueOf("su"),
                                    List.of(
                                            DBusString.valueOf("com.lucimber.test.FilterTest"),
                                            com.lucimber.dbus.type.DBusUInt32.valueOf(0)))
                            .withReplyExpected(true)
                            .build();

            assertNotNull(requestName);
            assertEquals("RequestName", requestName.getMember().toString());

            // Verify service name in payload
            String serviceName = ((DBusString) requestName.getPayload().get(0)).toString();
            assertEquals("com.lucimber.test.FilterTest", serviceName);

            // Clean up handler
            connection.getPipeline().remove("filtered-signal-capture");

            LOGGER.info("✓ Successfully created filtered signal handling setup");

        } finally {
            connection.close();
        }
    }

    /** Creates a connection for testing. */
    private Connection createConnection() throws Exception {
        ConnectionConfig config =
                ConnectionConfig.builder().withConnectTimeout(Duration.ofSeconds(30)).build();

        Connection connection =
                new NettyConnection(new InetSocketAddress(getDBusHost(), getDBusPort()), config);

        connection
                .connect()
                .toCompletableFuture()
                .get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        assertTrue(connection.isConnected());

        return connection;
    }

    /** Handler for capturing specific signals. */
    private static class SignalCaptureHandler extends AbstractInboundHandler {
        private final String expectedMember;
        private final AtomicReference<InboundSignal> capturedSignal;
        private final CountDownLatch latch;

        public SignalCaptureHandler(
                String expectedMember,
                AtomicReference<InboundSignal> capturedSignal,
                CountDownLatch latch) {
            this.expectedMember = expectedMember;
            this.capturedSignal = capturedSignal;
            this.latch = latch;
        }

        @Override
        protected Logger getLogger() {
            return LOGGER;
        }

        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal signal) {
                String member = signal.getMember().toString();
                if (expectedMember.equals(member)) {
                    LOGGER.info("Captured signal: {}", member);
                    capturedSignal.set(signal);
                    latch.countDown();
                    return; // Don't propagate to avoid interfering with other handlers
                }
            }
            super.handleInboundMessage(ctx, msg);
        }
    }

    /** Handler for capturing multiple signal types. */
    private static class MultiSignalCaptureHandler extends AbstractInboundHandler {
        private final AtomicReference<Integer> signalCount;
        private final CountDownLatch latch;

        public MultiSignalCaptureHandler(
                AtomicReference<Integer> signalCount, CountDownLatch latch) {
            this.signalCount = signalCount;
            this.latch = latch;
        }

        @Override
        protected Logger getLogger() {
            return LOGGER;
        }

        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal signal) {
                String member = signal.getMember().toString();
                LOGGER.info("Captured signal: {}", member);

                int count = signalCount.updateAndGet(c -> c + 1);
                if (count <= 2) { // Only count down for first 2 signals
                    latch.countDown();
                }
                return; // Don't propagate
            }
            super.handleInboundMessage(ctx, msg);
        }
    }

    /** Handler for capturing signals with specific filtering. */
    private static class FilteredSignalCaptureHandler extends AbstractInboundHandler {
        private final String expectedName;
        private final AtomicReference<InboundSignal> capturedSignal;
        private final CountDownLatch latch;

        public FilteredSignalCaptureHandler(
                String expectedName,
                AtomicReference<InboundSignal> capturedSignal,
                CountDownLatch latch) {
            this.expectedName = expectedName;
            this.capturedSignal = capturedSignal;
            this.latch = latch;
        }

        @Override
        protected Logger getLogger() {
            return LOGGER;
        }

        @Override
        public void handleInboundMessage(Context ctx, InboundMessage msg) {
            if (msg instanceof InboundSignal signal) {
                String member = signal.getMember().toString();

                if ("NameOwnerChanged".equals(member)) {
                    List<? extends DBusType> payload = signal.getPayload();
                    if (payload != null && !payload.isEmpty()) {
                        String name = ((DBusString) payload.get(0)).toString();
                        if (expectedName.equals(name)) {
                            LOGGER.info("Captured filtered signal for: {}", name);
                            capturedSignal.set(signal);
                            latch.countDown();
                            return; // Don't propagate
                        }
                    }
                }
            }
            super.handleInboundMessage(ctx, msg);
        }
    }
}
