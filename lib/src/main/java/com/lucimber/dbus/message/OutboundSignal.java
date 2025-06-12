/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.*;

import java.util.List;
import java.util.Optional;

/**
 * An outbound signal message.
 *
 * @since 1.0
 */
public final class OutboundSignal extends AbstractSignal implements OutboundMessage {

  private final DBusString dst;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial the serial number
   * @param path   the object path
   * @param iface  the name of the interface
   * @param member the name of this signal
   */
  public OutboundSignal(
        UInt32 serial,
        ObjectPath path,
        DBusString iface,
        DBusString member) {
    super(serial, path, iface, member);
    this.dst = null;
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial    the serial number
   * @param path      the object path
   * @param iface     the name of the interface
   * @param member    the name of this signal
   * @param dst       optional; the destination of this signal
   * @param signature optional; the signature of the message body
   * @param payload   optional; the message body
   */
  public OutboundSignal(
        UInt32 serial,
        ObjectPath path,
        DBusString iface,
        DBusString member,
        DBusString dst,
        Signature signature,
        List<? extends DBusType> payload) {
    super(serial, path, iface, member, signature, payload);
    this.dst = dst;
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(dst);
  }

  @Override
  public String toString() {
    var s = "OutboundSignal{dst='%s', serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var dst = getDestination().map(DBusString::toString).orElse("");
    var sig = getSignature().map(Signature::toString).orElse("");
    return String.format(s, dst, getSerial(), getObjectPath(), getInterfaceName(), getMember(), sig);
  }
}
