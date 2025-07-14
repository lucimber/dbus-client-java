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
 * An inbound method call message.
 *
 * @since 1.0
 */
public final class InboundMethodCall extends AbstractMethodCall implements InboundMessage {

  private final DBusString sender;
  private final boolean replyExpected;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial        the serial number
   * @param sender        the origin of this method call
   * @param path          the object path
   * @param member        the name of the method
   * @param replyExpected states if reply is expected
   */
  public InboundMethodCall(
          DBusUInt32 serial,
          DBusString sender,
          DBusObjectPath path,
          DBusString member,
          boolean replyExpected) {
    super(serial, path, member);
    this.sender = Objects.requireNonNull(sender);
    this.replyExpected = replyExpected;
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial        the serial number
   * @param sender        the origin of this method call
   * @param path          the object path
   * @param member        the name of the method
   * @param replyExpected states if reply is expected
   * @param iface         optional; the name of the interface
   * @param signature     optional; the signature of the message body
   * @param payload       optional; the message body
   */
  public InboundMethodCall(
          DBusUInt32 serial,
          DBusString sender,
          DBusObjectPath path,
          DBusString member,
          boolean replyExpected,
          DBusString iface,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, path, member, iface, signature, payload);
    this.sender = Objects.requireNonNull(sender);
    this.replyExpected = replyExpected;
  }

  @Override
  public DBusString getSender() {
    return sender;
  }

  /**
   * States if the sender expects a reply to this method call or not.
   *
   * @return {@code TRUE} if reply is expected, {@code FALSE} otherwise.
   */
  public boolean isReplyExpected() {
    return replyExpected;
  }

  @Override
  public String toString() {
    var s = "InboundMethodCall{sender='%s', serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var iface = getInterfaceName().map(DBusString::toString).orElse("");
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, sender, getSerial(), getObjectPath(), iface, getMember(), sig);
  }
}
