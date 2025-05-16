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
 * An inbound signal message.
 *
 * @since 1.0
 */
public final class InboundSignal extends AbstractSignal implements InboundMessage {

  private BetterDBusString sender;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial        the serial number
   * @param sender        the sender of this signal
   * @param objectPath    the object path
   * @param interfaceName the name of the D-Bus interface
   * @param name          the name of this signal
   */
  public InboundSignal(BetterDBusUInt32 serial, BetterDBusString sender, BetterDBusObjectPath objectPath,
                       BetterDBusString interfaceName, BetterDBusString name) {
    super(serial, objectPath, interfaceName, name);
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public BetterDBusString getSender() {
    return sender;
  }

  @Override
  public void setSender(BetterDBusString sender) {
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public String toString() {
    var s = "InboundSignal{sender='%s', serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
    return String.format(s, sender, getSerial(), getObjectPath(), getInterfaceName(), getName(), getSignature());
  }
}
