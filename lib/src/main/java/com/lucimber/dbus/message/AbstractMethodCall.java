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
import java.util.Optional;

/**
 * An abstract implementation of a method call message.
 *
 * @since 1.0
 */
abstract class AbstractMethodCall extends AbstractMessage {

  private final DBusObjectPath path;
  private final DBusString member;
  private final DBusString iface;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial the serial number
   * @param path   the object path
   * @param member the name of the method
   */
  AbstractMethodCall(
          DBusUInt32 serial,
          DBusObjectPath path,
          DBusString member) {
    super(serial);
    this.path = Objects.requireNonNull(path);
    this.member = Objects.requireNonNull(member);
    this.iface = null;
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial    the serial number
   * @param path      the object path
   * @param member    the name of the method
   * @param iface     optional; the name of the interface
   * @param signature optional; the signature of the message body
   * @param payload   optional; the message body
   */
  AbstractMethodCall(
          DBusUInt32 serial,
          DBusObjectPath path,
          DBusString member,
          DBusString iface,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, signature, payload);
    this.path = Objects.requireNonNull(path);
    this.member = Objects.requireNonNull(member);
    this.iface = iface;
  }

  /**
   * Gets the object path of this method.
   *
   * @return an {@link DBusObjectPath}
   */
  public DBusObjectPath getObjectPath() {
    return path;
  }

  /**
   * Gets the name of this method.
   *
   * @return a {@link DBusString}
   */
  public DBusString getMember() {
    return member;
  }

  /**
   * Gets the name of the interface, to which this method belongs.
   *
   * @return a {@link DBusString}
   */
  public Optional<DBusString> getInterfaceName() {
    return Optional.ofNullable(iface);
  }

  @Override
  public String toString() {
    var s = "AbstractMethodCall{serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var mappedIface = getInterfaceName().map(DBusString::toString).orElse("");
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, getSerial(), path, mappedIface, member, sig);
  }
}
