/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.ConnectionConfig;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.netty.NettyConnection;
import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for D-Bus service interaction and introspection capabilities. */
@Tag("integration")
@DisabledIf("shouldSkipDBusTests")
class ServiceInteractionIntegrationTest extends DBusIntegrationTestBase {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ServiceInteractionIntegrationTest.class);

    @Test
    void testServiceDiscovery() throws Exception {
        Connection connection = createConnection();

        try {
            // List all available services
            OutboundMethodCall listNames =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("ListNames"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> future =
                    connection.sendRequest(listNames).toCompletableFuture();
            InboundMessage response = future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(response);
            assertInstanceOf(InboundMethodReturn.class, response);

            InboundMethodReturn methodReturn = (InboundMethodReturn) response;
            List<? extends DBusType> payload = methodReturn.getPayload();
            assertNotNull(payload);

            DBusArray serviceNames = (DBusArray) payload.get(0);
            List<? extends DBusType> services = serviceNames;

            LOGGER.info("✓ Discovered {} D-Bus services", services.size());

            // Should find essential D-Bus services
            boolean foundDBusService =
                    services.stream()
                            .map(DBusString.class::cast)
                            .anyMatch(name -> "org.freedesktop.DBus".equals(name.toString()));
            assertTrue(foundDBusService, "Should find org.freedesktop.DBus service");

            // Log some discovered services for debugging
            services.stream()
                    .map(DBusString.class::cast)
                    .map(DBusString::toString)
                    .filter(name -> !name.startsWith(":")) // Filter out unique connection names
                    .limit(5)
                    .forEach(name -> LOGGER.info("  Found service: {}", name));

        } finally {
            connection.close();
        }
    }

    @Test
    void testServiceActivation() throws Exception {
        Connection connection = createConnection();

        try {
            // Try to start a service (this may fail if no activatable services exist)
            OutboundMethodCall listActivatableNames =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("ListActivatableNames"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> future =
                    connection.sendRequest(listActivatableNames).toCompletableFuture();
            InboundMessage response = future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(response);
            assertInstanceOf(InboundMethodReturn.class, response);

            InboundMethodReturn methodReturn = (InboundMethodReturn) response;
            List<? extends DBusType> payload = methodReturn.getPayload();
            assertNotNull(payload);

            DBusArray activatableNames = (DBusArray) payload.get(0);
            List<? extends DBusType> services = activatableNames;

            LOGGER.info("✓ Found {} activatable services", services.size());

            if (!services.isEmpty()) {
                services.stream()
                        .map(DBusString.class::cast)
                        .map(DBusString::toString)
                        .limit(3)
                        .forEach(name -> LOGGER.info("  Activatable service: {}", name));
            }

        } finally {
            connection.close();
        }
    }

    @Test
    void testDetailedIntrospection() throws Exception {
        Connection connection = createConnection();

        try {
            // Introspect the D-Bus daemon itself
            OutboundMethodCall introspect =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("Introspect"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(
                                    DBusString.valueOf("org.freedesktop.DBus.Introspectable"))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> future =
                    connection.sendRequest(introspect).toCompletableFuture();
            InboundMessage response = future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(response);
            assertInstanceOf(InboundMethodReturn.class, response);

            InboundMethodReturn methodReturn = (InboundMethodReturn) response;
            List<? extends DBusType> payload = methodReturn.getPayload();
            assertNotNull(payload);

            DBusString xmlData = (DBusString) payload.get(0);
            String xml = xmlData.toString();

            // Detailed validation of introspection XML
            assertTrue(xml.contains("<!DOCTYPE"), "Should contain XML DOCTYPE declaration");
            assertTrue(xml.contains("<node"), "Should contain root node element");
            assertTrue(xml.contains("</node>"), "Should have properly closed node element");

            // Should contain standard D-Bus interfaces
            assertTrue(xml.contains("org.freedesktop.DBus"), "Should contain D-Bus interface");
            assertTrue(
                    xml.contains("org.freedesktop.DBus.Introspectable"),
                    "Should contain Introspectable interface");
            assertTrue(xml.contains("org.freedesktop.DBus.Peer"), "Should contain Peer interface");

            // Should contain method definitions
            assertTrue(xml.contains("<method"), "Should contain method definitions");
            assertTrue(xml.contains("ListNames"), "Should contain ListNames method");
            assertTrue(xml.contains("Introspect"), "Should contain Introspect method");

            // Check for argument definitions
            assertTrue(xml.contains("<arg"), "Should contain argument definitions");

            LOGGER.info("✓ Successfully validated detailed introspection data");
            LOGGER.info("  XML length: {} characters", xml.length());
            LOGGER.info("  Contains {} method definitions", countOccurrences(xml, "<method"));
            LOGGER.info("  Contains {} interface definitions", countOccurrences(xml, "<interface"));

        } finally {
            connection.close();
        }
    }

    @Test
    void testServiceOwnership() throws Exception {
        Connection connection = createConnection();

        try {
            String testServiceName = "com.lucimber.test.ServiceOwnershipTest";

            // Request ownership of a test service name
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
                                            DBusString.valueOf(testServiceName),
                                            DBusUInt32.valueOf(0) // No flags
                                            ))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> future =
                    connection.sendRequest(requestName).toCompletableFuture();
            InboundMessage response = future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(response);
            assertInstanceOf(InboundMethodReturn.class, response);

            InboundMethodReturn methodReturn = (InboundMethodReturn) response;
            List<? extends DBusType> payload = methodReturn.getPayload();
            assertNotNull(payload);

            DBusUInt32 result = (DBusUInt32) payload.get(0);
            long resultCode = result.longValue();

            // Result codes: 1=PRIMARY_OWNER, 2=IN_QUEUE, 3=EXISTS, 4=ALREADY_OWNER
            assertTrue(
                    resultCode >= 1 && resultCode <= 4,
                    "Should get valid RequestName result code: " + resultCode);

            LOGGER.info(
                    "✓ Successfully requested service name: {} (result: {})",
                    testServiceName,
                    resultCode);

            // Verify we can check ownership
            OutboundMethodCall nameHasOwner =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("NameHasOwner"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(
                                    DBusSignature.valueOf("s"),
                                    List.of(DBusString.valueOf(testServiceName)))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> checkFuture =
                    connection.sendRequest(nameHasOwner).toCompletableFuture();
            InboundMessage checkResponse =
                    checkFuture.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(checkResponse);
            assertInstanceOf(InboundMethodReturn.class, checkResponse);

            InboundMethodReturn checkReturn = (InboundMethodReturn) checkResponse;
            List<? extends DBusType> checkPayload = checkReturn.getPayload();

            com.lucimber.dbus.type.DBusBoolean hasOwner =
                    (com.lucimber.dbus.type.DBusBoolean) checkPayload.get(0);
            assertTrue(hasOwner.getDelegate(), "Service should have an owner after requesting it");

            LOGGER.info("✓ Verified service ownership check");

            // Release the name
            OutboundMethodCall releaseName =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("ReleaseName"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(
                                    DBusSignature.valueOf("s"),
                                    List.of(DBusString.valueOf(testServiceName)))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> releaseFuture =
                    connection.sendRequest(releaseName).toCompletableFuture();
            InboundMessage releaseResponse =
                    releaseFuture.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(releaseResponse);
            LOGGER.info("✓ Successfully released service name");

        } finally {
            connection.close();
        }
    }

    @Test
    void testConnectionUniqueName() throws Exception {
        Connection connection = createConnection();

        try {
            // First, get our unique name by querying who owns a well-known name
            // Since every connection has the special name "org.freedesktop.DBus", we can use
            // GetNameOwner
            // But actually, let's first list all names and find our unique name
            OutboundMethodCall listNames =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("ListNames"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withReplyExpected(true)
                            .build();

            CompletableFuture<InboundMessage> future =
                    connection.sendRequest(listNames).toCompletableFuture();
            InboundMessage response = future.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            assertNotNull(response);
            assertInstanceOf(InboundMethodReturn.class, response);

            InboundMethodReturn methodReturn = (InboundMethodReturn) response;
            List<? extends DBusType> payload = methodReturn.getPayload();
            assertNotNull(payload);

            DBusArray serviceNames = (DBusArray) payload.get(0);
            List<? extends DBusType> names = serviceNames;

            // Find a unique name (starts with ':')
            String name = null;
            for (DBusType nameType : names) {
                String serviceName = ((DBusString) nameType).toString();
                if (serviceName.startsWith(":")) {
                    // This is a unique name, could be ours
                    name = serviceName;
                    break;
                }
            }

            assertNotNull(name);
            assertTrue(name.startsWith(":"), "Unique name should start with ':'");
            assertTrue(name.length() > 2, "Unique name should have meaningful content");

            LOGGER.info("✓ Connection unique name: {}", name);

            // Verify we can get information about our own connection
            OutboundMethodCall getConnectionUnixUser =
                    OutboundMethodCall.Builder.create()
                            .withSerial(connection.getNextSerial())
                            .withPath(DBusObjectPath.valueOf("/org/freedesktop/DBus"))
                            .withMember(DBusString.valueOf("GetConnectionUnixUser"))
                            .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                            .withInterface(DBusString.valueOf("org.freedesktop.DBus"))
                            .withBody(DBusSignature.valueOf("s"), List.of(DBusString.valueOf(name)))
                            .withReplyExpected(true)
                            .build();

            try {
                CompletableFuture<InboundMessage> userFuture =
                        connection.sendRequest(getConnectionUnixUser).toCompletableFuture();
                InboundMessage userResponse =
                        userFuture.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

                if (userResponse instanceof InboundMethodReturn userReturn) {
                    List<? extends DBusType> userPayload = userReturn.getPayload();
                    DBusUInt32 userId = (DBusUInt32) userPayload.get(0);
                    LOGGER.info("✓ Connection Unix user ID: {}", userId.longValue());
                }
            } catch (Exception e) {
                // This might not be supported on all systems
                LOGGER.info("GetConnectionUnixUser not supported: {}", e.getMessage());
            }

        } finally {
            connection.close();
        }
    }

    @Test
    void testMultipleObjectPaths() throws Exception {
        Connection connection = createConnection();

        try {
            // Test introspection of root path
            String[] paths = {"/", "/org", "/org/freedesktop", "/org/freedesktop/DBus"};

            for (String path : paths) {
                try {
                    OutboundMethodCall introspect =
                            OutboundMethodCall.Builder.create()
                                    .withSerial(connection.getNextSerial())
                                    .withPath(DBusObjectPath.valueOf(path))
                                    .withMember(DBusString.valueOf("Introspect"))
                                    .withDestination(DBusString.valueOf("org.freedesktop.DBus"))
                                    .withInterface(
                                            DBusString.valueOf(
                                                    "org.freedesktop.DBus.Introspectable"))
                                    .withReplyExpected(true)
                                    .build();

                    CompletableFuture<InboundMessage> future =
                            connection.sendRequest(introspect).toCompletableFuture();
                    InboundMessage response = future.get(10, TimeUnit.SECONDS);

                    if (response instanceof InboundMethodReturn methodReturn) {
                        List<? extends DBusType> payload = methodReturn.getPayload();
                        DBusString xmlData = (DBusString) payload.get(0);
                        String xml = xmlData.toString();

                        LOGGER.info(
                                "✓ Successfully introspected path: {} ({} chars)",
                                path,
                                xml.length());

                        // Basic validation
                        assertTrue(
                                xml.contains("<node"),
                                "Should contain node element for path: " + path);
                    }

                } catch (Exception e) {
                    LOGGER.debug(
                            "Introspection failed for path {} (may be expected): {}",
                            path,
                            e.getMessage());
                }
            }

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

    /** Count occurrences of a substring in a string. */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
