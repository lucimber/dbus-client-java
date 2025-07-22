/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.standard;

import com.lucimber.dbus.type.DBusString;

/**
 * The {@literal org.freedesktop.DBus.Peer} interface provides basic peer-to-peer
 * functionality between D-Bus connections. This interface is typically implemented
 * by all D-Bus objects and provides fundamental operations.
 * 
 * <p>The Peer interface is often used for:</p>
 * <ul>
 *   <li>Testing connectivity between D-Bus peers</li>
 *   <li>Obtaining machine identifiers for security or logging</li>
 *   <li>Basic heartbeat/keepalive functionality</li>
 * </ul>
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces-peer">D-Bus Specification - Peer</a>
 */
public interface Peer {
  /**
   * Pings the peer to test connectivity.
   * 
   * <p>This method does nothing and returns nothing. It is primarily used to test
   * whether the target object exists and is reachable. If this method returns
   * without error, the connection is working properly.</p>
   * 
   * <p>Common uses include:</p>
   * <ul>
   *   <li>Connection health checks</li>
   *   <li>Keepalive messages to prevent connection timeout</li>
   *   <li>Verifying that a service is running</li>
   * </ul>
   */
  void ping();

  /**
   * Gets the machine ID of the peer.
   * 
   * <p>The machine ID is a unique identifier for the machine where the peer is running.
   * This is typically a randomly-generated UUID that remains stable across reboots.
   * On Linux systems, this usually corresponds to the contents of {@code /etc/machine-id}
   * or {@code /var/lib/dbus/machine-id}.</p>
   * 
   * <p>The machine ID is useful for:</p>
   * <ul>
   *   <li>Identifying remote machines in distributed systems</li>
   *   <li>Security logging and auditing</li>
   *   <li>License management and node counting</li>
   * </ul>
   *
   * @return A {@link DBusString} containing the machine ID (typically a 32-character hex string)
   */
  DBusString getMachineId();
}