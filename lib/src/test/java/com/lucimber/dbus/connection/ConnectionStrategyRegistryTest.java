/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectionStrategyRegistryTest {

    private ConnectionStrategyRegistry registry;
    private MockConnectionStrategy unixStrategy;
    private MockConnectionStrategy tcpStrategy;
    private MockConnectionStrategy unavailableStrategy;

    @BeforeEach
    void setUp() {
        registry = new ConnectionStrategyRegistry();
        unixStrategy = new MockConnectionStrategy("Unix", DomainSocketAddress.class, true);
        tcpStrategy = new MockConnectionStrategy("TCP", InetSocketAddress.class, true);
        unavailableStrategy =
                new MockConnectionStrategy("Unavailable", DomainSocketAddress.class, false);
    }

    @Test
    void testRegisterStrategy() {
        registry.registerStrategy(unixStrategy);

        List<ConnectionStrategy> strategies = registry.getAllStrategies();
        assertEquals(1, strategies.size());
        assertTrue(strategies.contains(unixStrategy));
    }

    @Test
    void testRegisterMultipleStrategies() {
        registry.registerStrategy(unixStrategy);
        registry.registerStrategy(tcpStrategy);

        List<ConnectionStrategy> strategies = registry.getAllStrategies();
        assertEquals(2, strategies.size());
        assertTrue(strategies.contains(unixStrategy));
        assertTrue(strategies.contains(tcpStrategy));
    }

    @Test
    void testFindStrategyForUnixSocket() {
        registry.registerStrategy(unixStrategy);
        registry.registerStrategy(tcpStrategy);

        SocketAddress address = new DomainSocketAddress("/tmp/test");
        Optional<ConnectionStrategy> strategy = registry.findStrategy(address);

        assertTrue(strategy.isPresent());
        assertEquals(unixStrategy, strategy.get());
    }

    @Test
    void testFindStrategyForTcpSocket() {
        registry.registerStrategy(unixStrategy);
        registry.registerStrategy(tcpStrategy);

        SocketAddress address = new InetSocketAddress("localhost", 8080);
        Optional<ConnectionStrategy> strategy = registry.findStrategy(address);

        assertTrue(strategy.isPresent());
        assertEquals(tcpStrategy, strategy.get());
    }

    @Test
    void testFindStrategyUnavailable() {
        registry.registerStrategy(unavailableStrategy);

        SocketAddress address = new DomainSocketAddress("/tmp/test");
        Optional<ConnectionStrategy> strategy = registry.findStrategy(address);

        assertFalse(strategy.isPresent());
    }

    @Test
    void testFindStrategyNoMatch() {
        registry.registerStrategy(unixStrategy);

        SocketAddress address = new InetSocketAddress("localhost", 8080);
        Optional<ConnectionStrategy> strategy = registry.findStrategy(address);

        assertFalse(strategy.isPresent());
    }

    @Test
    void testGetAvailableStrategies() {
        registry.registerStrategy(unixStrategy);
        registry.registerStrategy(tcpStrategy);
        registry.registerStrategy(unavailableStrategy);

        List<ConnectionStrategy> available = registry.getAvailableStrategies();
        assertEquals(2, available.size());
        assertTrue(available.contains(unixStrategy));
        assertTrue(available.contains(tcpStrategy));
        assertFalse(available.contains(unavailableStrategy));
    }

    @Test
    void testRegisterNullStrategy() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    registry.registerStrategy(null);
                });
    }

    @Test
    void testFindStrategyNullAddress() {
        assertThrows(
                NullPointerException.class,
                () -> {
                    registry.findStrategy(null);
                });
    }

    // Mock strategy for testing
    private static class MockConnectionStrategy implements ConnectionStrategy {
        private final String name;
        private final Class<? extends SocketAddress> supportedType;
        private final boolean available;

        MockConnectionStrategy(
                String name, Class<? extends SocketAddress> supportedType, boolean available) {
            this.name = name;
            this.supportedType = supportedType;
            this.available = available;
        }

        @Override
        public boolean supports(SocketAddress address) {
            return supportedType.isInstance(address);
        }

        @Override
        public CompletionStage<ConnectionHandle> connect(
                SocketAddress address, ConnectionConfig config, ConnectionContext context) {
            throw new UnsupportedOperationException("Mock strategy");
        }

        @Override
        public String getTransportName() {
            return name;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }
    }
}
