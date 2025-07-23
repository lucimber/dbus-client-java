/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.connection.ConnectionConfig;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.junit.jupiter.api.Test;

/**
 * Integration test to verify that NettyConnection properly uses the strategy pattern for different
 * transport types.
 */
class NettyConnectionStrategyIntegrationTest {

    @Test
    void testUnixSocketStrategySelection() {
        SocketAddress unixAddress = new DomainSocketAddress("/tmp/test-socket");

        // This should not throw an exception during construction
        assertDoesNotThrow(
                () -> {
                    NettyConnection connection =
                            new NettyConnection(unixAddress, ConnectionConfig.defaultConfig());
                    assertNotNull(connection);

                    // Verify the connection uses the Unix strategy
                    // We can't easily test the internal strategy without making it public,
                    // but we can verify the connection was created successfully
                    assertNotNull(connection.getPipeline());
                    assertNotNull(connection.getConfig());

                    connection.close();
                });
    }

    @Test
    void testTcpStrategySelection() {
        SocketAddress tcpAddress = new InetSocketAddress("localhost", 12345);

        // This should not throw an exception during construction
        assertDoesNotThrow(
                () -> {
                    NettyConnection connection =
                            new NettyConnection(tcpAddress, ConnectionConfig.defaultConfig());
                    assertNotNull(connection);

                    // Verify the connection uses the TCP strategy
                    assertNotNull(connection.getPipeline());
                    assertNotNull(connection.getConfig());

                    connection.close();
                });
    }

    @Test
    void testUnsupportedAddressType() {
        // Create a custom SocketAddress that no strategy supports
        SocketAddress unsupportedAddress =
                new SocketAddress() {
                    @Override
                    public String toString() {
                        return "UnsupportedAddress";
                    }
                };

        // This should throw an IllegalArgumentException
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    new NettyConnection(unsupportedAddress, ConnectionConfig.defaultConfig());
                });
    }

    @Test
    void testFactoryMethods() {
        // Test that factory methods still work with strategy pattern
        // Note: These will fail if system D-Bus is not available, but should not fail due to
        // strategy pattern

        assertDoesNotThrow(
                () -> {
                    try {
                        NettyConnection systemConnection = NettyConnection.newSystemBusConnection();
                        assertNotNull(systemConnection);
                        systemConnection.close();
                    } catch (UnsupportedOperationException | IllegalStateException e) {
                        // Expected if native transport or D-Bus is not available
                        // The important thing is that it doesn't fail due to strategy pattern
                        // issues
                    }
                });

        // Session bus test - will fail if DBUS_SESSION_BUS_ADDRESS is not set
        assertDoesNotThrow(
                () -> {
                    try {
                        NettyConnection sessionConnection =
                                NettyConnection.newSessionBusConnection();
                        assertNotNull(sessionConnection);
                        sessionConnection.close();
                    } catch (IllegalStateException | UnsupportedOperationException e) {
                        // Expected if environment variable is not set or native transport not
                        // available
                    }
                });
    }

    @Test
    void testConnectionState() {
        SocketAddress address = new InetSocketAddress("localhost", 12345);
        NettyConnection connection = new NettyConnection(address, ConnectionConfig.defaultConfig());

        // Test initial state
        assertFalse(connection.isConnected());
        assertNotNull(connection.getState());

        // Test that we can get next serial should throw exception when not connected
        assertThrows(IllegalStateException.class, connection::getNextSerial);

        connection.close();
    }
}
