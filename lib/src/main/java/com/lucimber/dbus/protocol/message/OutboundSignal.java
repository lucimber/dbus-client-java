/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusObjectPath;
import com.lucimber.dbus.protocol.types.BetterDBusString;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Optional;

/**
 * An outbound signal message.
 *
 * @since 1.0
 */
public final class OutboundSignal extends AbstractSignal implements OutboundMessage {

  private BetterDBusString destination;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial        the serial number
   * @param objectPath    the object path
   * @param interfaceName the name of the D-Bus interface
   * @param name          the name of this signal
   */
  public OutboundSignal(BetterDBusUInt32 serial, BetterDBusObjectPath objectPath, BetterDBusString interfaceName,
                        BetterDBusString name) {
    super(serial, objectPath, interfaceName, name);
  }

  @Override
  public Optional<BetterDBusString> getDestination() {
    return Optional.ofNullable(destination);
  }

  @Override
  public void setDestination(BetterDBusString destination) {
    this.destination = destination;
  }

  @Override
  public String toString() {
    var s = "OutboundSignal{destination='%s', serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
    return String.format(s, destination, getSerial(), getObjectPath(), getInterfaceName(), getName(),
          getSignature());
  }
}
