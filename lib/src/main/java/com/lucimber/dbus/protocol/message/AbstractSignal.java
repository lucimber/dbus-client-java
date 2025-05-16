/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusObjectPath;
import com.lucimber.dbus.protocol.types.BetterDBusString;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Objects;

/**
 * @since 1.0
 */
abstract class AbstractSignal extends AbstractMessage {

  private BetterDBusObjectPath objectPath;
  private BetterDBusString name;
  private BetterDBusString interfaceName;

  AbstractSignal(BetterDBusUInt32 serial, BetterDBusObjectPath objectPath,
                 BetterDBusString interfaceName, BetterDBusString name) {
    super(serial);
    this.objectPath = Objects.requireNonNull(objectPath);
    this.interfaceName = Objects.requireNonNull(interfaceName);
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Gets the object path of this signal.
   *
   * @return The path as an {@link BetterDBusObjectPath}.
   */
  public BetterDBusObjectPath getObjectPath() {
    return objectPath;
  }

  /**
   * Sets the object path of this signal.
   *
   * @param objectPath an {@link BetterDBusObjectPath}
   */
  public void setObjectPath(BetterDBusObjectPath objectPath) {
    this.objectPath = Objects.requireNonNull(objectPath);
  }

  /**
   * Gets the name of this signal.
   *
   * @return a {@link BetterDBusString}
   */
  public BetterDBusString getName() {
    return name;
  }

  /**
   * Sets the name of this signal.
   *
   * @param name a {@link BetterDBusString}
   */
  public void setName(BetterDBusString name) {
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Gets the name of the interface, to which this signal belongs.
   *
   * @return a {@link BetterDBusString}
   */
  public BetterDBusString getInterfaceName() {
    return interfaceName;
  }

  /**
   * Sets the name of the interface, to which this signal belongs.
   *
   * @param interfaceName a {@link BetterDBusString}
   */
  public void setInterfaceName(BetterDBusString interfaceName) {
    this.interfaceName = Objects.requireNonNull(interfaceName);
  }

  @Override
  public String toString() {
    var s = "AbstractSignal{serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
    return String.format(s, getSerial(), objectPath, interfaceName, name, getSignature());
  }
}
