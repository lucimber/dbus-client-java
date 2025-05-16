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
 * An outbound method call.
 *
 * @since 1.0
 */
public final class OutboundMethodCall extends AbstractMethodCall implements OutboundMessage {

  private BetterDBusString destination;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param destination the destination of this method call
   * @param objectPath  the object path
   * @param name        the name of the method
   */
  public OutboundMethodCall(BetterDBusUInt32 serial, BetterDBusString destination,
                            BetterDBusObjectPath objectPath, BetterDBusString name) {
    super(serial, objectPath, name);
    this.destination = Objects.requireNonNull(destination);
  }

  @Override
  public Optional<BetterDBusString> getDestination() {
    return Optional.of(destination);
  }

  @Override
  public void setDestination(BetterDBusString destination) {
    this.destination = Objects.requireNonNull(destination);
  }

  @Override
  public String toString() {
    var s = "OutboundMethodCall{destination='%s', serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
    return String.format(s, destination, getSerial(), getObjectPath(), getInterfaceName(), getName(),
          getSignature());
  }
}
