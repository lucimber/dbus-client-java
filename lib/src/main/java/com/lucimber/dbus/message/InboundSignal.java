/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.List;
import java.util.Objects;

/**
 * An inbound signal message.
 *
 * @since 1.0
 */
public final class InboundSignal extends AbstractSignal implements InboundMessage {

  private final DBusString sender;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial the serial number
   * @param sender the sender of this signal
   * @param path   the object path
   * @param iface  the name of the D-Bus interface
   * @param member the name of this signal
   */
  public InboundSignal(
          DBusUInt32 serial,
          DBusString sender,
          DBusObjectPath path,
          DBusString iface,
          DBusString member) {
    super(serial, path, iface, member);
    this.sender = Objects.requireNonNull(sender);
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial    the serial number
   * @param sender    the sender of this signal
   * @param path      the object path
   * @param iface     the name of the D-Bus interface
   * @param member    the name of this signal
   * @param signature optional; the signature of the message body
   * @param payload   optional; the message body
   */
  public InboundSignal(
          DBusUInt32 serial,
          DBusString sender,
          DBusObjectPath path,
          DBusString iface,
          DBusString member,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, path, iface, member, signature, payload);
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public DBusString getSender() {
    return sender;
  }

  @Override
  public String toString() {
    var s = "InboundSignal{sender='%s', serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, sender, getSerial(), getObjectPath(), getInterfaceName(), getMember(), sig);
  }
}
