/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusObjectPath;
import com.lucimber.dbus.protocol.types.BetterDBusString;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Objects;
import java.util.Optional;

/**
 * @since 1.0
 */
abstract class AbstractMethodCall extends AbstractMessage {

  private BetterDBusObjectPath objectPath;
  private BetterDBusString name;
  private BetterDBusString interfaceName;

  AbstractMethodCall(BetterDBusUInt32 serial, BetterDBusObjectPath objectPath, BetterDBusString name) {
    super(serial);
    this.objectPath = Objects.requireNonNull(objectPath);
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Gets the object path of this method.
   *
   * @return an {@link BetterDBusObjectPath}
   */
  public BetterDBusObjectPath getObjectPath() {
    return objectPath;
  }

  /**
   * Sets the object path of this method.
   *
   * @param objectPath an {@link BetterDBusObjectPath}
   */
  public void setObjectPath(BetterDBusObjectPath objectPath) {
    this.objectPath = Objects.requireNonNull(objectPath);
  }

  /**
   * Gets the name of this method.
   *
   * @return a {@link BetterDBusString}
   */
  public BetterDBusString getName() {
    return name;
  }

  /**
   * Sets the name of this method.
   *
   * @param name a {@link BetterDBusString}
   */
  public void setName(BetterDBusString name) {
    this.name = Objects.requireNonNull(name);
  }

  /**
   * Gets the name of the interface, to which this method belongs.
   *
   * @return a {@link BetterDBusString}
   */
  public Optional<BetterDBusString> getInterfaceName() {
    return Optional.ofNullable(interfaceName);
  }

  /**
   * Sets the name of the interface, to which this method belongs.
   *
   * @param interfaceName a {@link BetterDBusString}
   */
  public void setInterfaceName(BetterDBusString interfaceName) {
    this.interfaceName = interfaceName;
  }

  @Override
  public String toString() {
    var s = "AbstractMethodCall{serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
    return String.format(s, getSerial(), objectPath, interfaceName, name, getSignature());
  }
}
