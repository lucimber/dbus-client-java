/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An outbound error message.
 *
 * @since 1.0
 */
public final class OutboundError extends AbstractReply implements OutboundMessage, Reply {

  private final DBusString dst;
  private final DBusString errorName;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param errorName   the name of this error
   */
  public OutboundError(
          UInt32 serial,
          UInt32 replySerial,
          DBusString errorName) {
    super(serial, replySerial);
    this.errorName = Objects.requireNonNull(errorName);
    this.dst = null;
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param errorName   the name of this error
   * @param dst         optional; the destination of this error
   * @param signature   optional; the signature of the message body
   * @param payload     optional; the message body
   */
  public OutboundError(
          UInt32 serial,
          UInt32 replySerial,
          DBusString errorName,
          DBusString dst,
          Signature signature,
          List<? extends DBusType> payload) {
    super(serial, replySerial, signature, payload);
    this.errorName = Objects.requireNonNull(errorName);
    this.dst = dst;
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(dst);
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
    var s = "OutboundError{destination='%s', serial='%s', replySerial='%s', name='%s', sig='%s'}";
    var mappedDst = getDestination().map(DBusString::toString).orElse("");
    var sig = getSignature().map(Signature::toString).orElse("");
    return String.format(s, mappedDst, getSerial(), getReplySerial(), getErrorName(), sig);
  }
}
