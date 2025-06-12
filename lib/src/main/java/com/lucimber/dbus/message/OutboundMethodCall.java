/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.*;

import java.util.List;
import java.util.Optional;

/**
 * An outbound method call.
 *
 * @since 1.0
 */
public final class OutboundMethodCall extends AbstractMethodCall implements OutboundMessage {

  private final DBusString dst;
  private final boolean replyExpected;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial        the serial number
   * @param path          the object path
   * @param member        the name of the method
   * @param replyExpected states if reply is expected
   */
  public OutboundMethodCall(
        UInt32 serial,
        ObjectPath path,
        DBusString member,
        boolean replyExpected) {
    super(serial, path, member);
    this.dst = null;
    this.replyExpected = replyExpected;
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial        the serial number
   * @param path          the object path
   * @param member        the name of the method
   * @param replyExpected states if reply is expected
   * @param dst           optional; the destination of this method call
   * @param iface         optional; the name of the interface
   * @param signature     optional; the signature of the message body
   * @param payload       optional; the message body
   */
  public OutboundMethodCall(
        UInt32 serial,
        ObjectPath path,
        DBusString member,
        boolean replyExpected,
        DBusString dst,
        DBusString iface,
        Signature signature,
        List<? extends DBusType> payload) {
    super(serial, path, member, iface, signature, payload);
    this.dst = dst;
    this.replyExpected = replyExpected;
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(dst);
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
    var s = "OutboundMethodCall{dst='%s', serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var dst = getDestination().map(DBusString::toString).orElse("");
    var iface = getInterfaceName().map(DBusString::toString).orElse("");
    var sig = getSignature().map(Signature::toString).orElse("");
    return String.format(s, dst, getSerial(), getObjectPath(), iface, getMember(), sig);
  }
}
