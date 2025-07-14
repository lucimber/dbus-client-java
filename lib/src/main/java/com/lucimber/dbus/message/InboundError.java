/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.List;
import java.util.Objects;

/**
 * An inbound error message.
 *
 * @since 1.0
 */
public final class InboundError extends AbstractReply implements InboundReply {

  private final DBusString sender;
  private final DBusString errorName;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param sender      the origin of this error message
   * @param errorName   the name of the error
   */
  public InboundError(
          DBusUInt32 serial,
          DBusUInt32 replySerial,
          DBusString sender,
          DBusString errorName) {
    super(serial, replySerial);
    this.sender = Objects.requireNonNull(sender);
    this.errorName = Objects.requireNonNull(errorName);
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param sender      the origin of this error message
   * @param errorName   the name of the error
   * @param signature   optional; the signature of the message body
   * @param payload     optional; the message body
   */
  public InboundError(
          DBusUInt32 serial,
          DBusUInt32 replySerial,
          DBusString sender,
          DBusString errorName,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, replySerial, signature, payload);
    this.sender = Objects.requireNonNull(sender);
    this.errorName = Objects.requireNonNull(errorName);
  }

  @Override
  public DBusString getSender() {
    return sender;
  }

  /**
   * Returns the name of this error.
   *
   * @return a {@link DBusString}
   */
  public DBusString getErrorName() {
    return errorName;
  }

  @Override
  public String toString() {
    var s = "InboundError{sender='%s', serial='%s', replySerial='%s', name='%s', sig='%s'}";
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, sender, getSerial(), getReplySerial(), errorName, sig);
  }
}
