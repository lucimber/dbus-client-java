/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry for connection strategies that automatically selects the appropriate strategy based on
 * the socket address type and platform availability.
 *
 * <p>This registry allows pluggable transport implementations while maintaining a clean abstraction
 * for connection creation.
 */
public final class ConnectionStrategyRegistry {

    private final List<ConnectionStrategy> strategies;

    /** Creates a new registry with the default set of strategies. */
    public ConnectionStrategyRegistry() {
        this.strategies = new ArrayList<>();
    }

    /**
     * Creates a new registry with the specified strategies.
     *
     * @param strategies the initial list of strategies
     */
    public ConnectionStrategyRegistry(List<ConnectionStrategy> strategies) {
        this.strategies = new ArrayList<>(Objects.requireNonNull(strategies));
    }

    /**
     * Registers a new connection strategy.
     *
     * @param strategy the strategy to register
     */
    public void registerStrategy(ConnectionStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        strategies.add(strategy);
    }

    /**
     * Finds the first available strategy that supports the given socket address.
     *
     * @param address the socket address to find a strategy for
     * @return the strategy if found, empty otherwise
     */
    public Optional<ConnectionStrategy> findStrategy(SocketAddress address) {
        Objects.requireNonNull(address, "address must not be null");

        return strategies.stream()
                .filter(strategy -> strategy.supports(address))
                .filter(ConnectionStrategy::isAvailable)
                .findFirst();
    }

    /**
     * Gets all registered strategies.
     *
     * @return unmodifiable list of all strategies
     */
    public List<ConnectionStrategy> getAllStrategies() {
        return List.copyOf(strategies);
    }

    /**
     * Gets all available strategies (those that can be used on this platform).
     *
     * @return list of available strategies
     */
    public List<ConnectionStrategy> getAvailableStrategies() {
        return strategies.stream().filter(ConnectionStrategy::isAvailable).toList();
    }
}
