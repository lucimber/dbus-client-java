/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.UInt32;

import java.util.List;
import java.util.Optional;

/**
 * An outbound method return message.
 *
 * @since 1.0
 */
public final class OutboundMethodReturn extends AbstractReply implements OutboundMessage, Reply {

  private final DBusString dst;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   */
  public OutboundMethodReturn(
        UInt32 serial,
        UInt32 replySerial) {
    super(serial, replySerial);
    this.dst = null;
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param dst         optional; the destination of this method return
   * @param signature   optional; the signature of the message body
   * @param payload     optional; the message body
   */
  public OutboundMethodReturn(
        UInt32 serial,
        UInt32 replySerial,
        DBusString dst,
        Signature signature,
        List<? extends DBusType> payload) {
    super(serial, replySerial, signature, payload);
    this.dst = dst;
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(dst);
  }

  @Override
  public String toString() {
    var s = "OutboundMethodReturn{dst='%s', serial='%s', replySerial='%s', sig=%s}";
    var mappedDst = getDestination().map(DBusString::toString).orElse("");
    var sig = getSignature().map(Signature::toString).orElse("");
    return String.format(s, mappedDst, getSerial(), getReplySerial(), sig);
  }
}
