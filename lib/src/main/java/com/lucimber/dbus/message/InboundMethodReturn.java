/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.List;
import java.util.Objects;

/**
 * An inbound method return message.
 *
 * @since 1.0
 */
public final class InboundMethodReturn extends AbstractReply implements InboundReply {

  private final DBusString sender;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param sender      the origin of this method return
   */
  public InboundMethodReturn(
          DBusUInt32 serial,
          DBusUInt32 replySerial,
          DBusString sender) {
    super(serial, replySerial);
    this.sender = Objects.requireNonNull(sender);
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param sender      the origin of this method return
   * @param signature   optional; the signature of the message body
   * @param payload     optional; the message body
   */
  public InboundMethodReturn(
          DBusUInt32 serial,
          DBusUInt32 replySerial,
          DBusString sender,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, replySerial, signature, payload);
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public DBusString getSender() {
    return sender;
  }

  @Override
  public String toString() {
    var s = "InboundMethodReturn{sender='%s', serial='%s', replySerial='%s', sig='%s'}";
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, sender, getSerial(), getReplySerial(), sig);
  }
}
