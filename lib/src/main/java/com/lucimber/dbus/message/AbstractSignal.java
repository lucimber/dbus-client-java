/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.List;
import java.util.Objects;

/**
 * An abstract implementation of a signal message.
 *
 * @since 1.0
 */
abstract class AbstractSignal extends AbstractMessage {

  private final DBusObjectPath path;
  private final DBusString member;
  private final DBusString iface;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial the serial number
   * @param path   the object path
   * @param iface  the name of the interface
   * @param member the name of the signal
   */
  AbstractSignal(
          DBusUInt32 serial,
          DBusObjectPath path,
          DBusString iface,
          DBusString member) {
    this(serial, path, iface, member, null, null);
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial    the serial number
   * @param path      the object path
   * @param iface     the name of the interface
   * @param member    the name of the signal
   * @param signature optional; the signature of the message body
   * @param payload   optional; the message body
   */
  AbstractSignal(
          DBusUInt32 serial,
          DBusObjectPath path,
          DBusString iface,
          DBusString member,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, signature, payload);
    this.path = Objects.requireNonNull(path);
    this.iface = Objects.requireNonNull(iface);
    this.member = Objects.requireNonNull(member);
  }

  /**
   * Gets the object path of this signal.
   *
   * @return The path as an {@link DBusObjectPath}.
   */
  public DBusObjectPath getObjectPath() {
    return path;
  }

  /**
   * Gets the name of this signal.
   *
   * @return a {@link DBusString}
   */
  public DBusString getMember() {
    return member;
  }

  /**
   * Gets the name of the interface, to which this signal belongs.
   *
   * @return a {@link DBusString}
   */
  public DBusString getInterfaceName() {
    return iface;
  }

  @Override
  public String toString() {
    var s = "AbstractSignal{serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, getSerial(), path, iface, member, sig);
  }
}
