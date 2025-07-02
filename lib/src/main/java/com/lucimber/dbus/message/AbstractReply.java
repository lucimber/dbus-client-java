/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import java.util.List;
import java.util.Objects;

/**
 * An abstract implementation of a reply message.
 *
 * @since 1.0
 */
abstract class AbstractReply extends AbstractMessage implements Reply {

  private final UInt32 replySerial;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   */
  AbstractReply(
          UInt32 serial,
          UInt32 replySerial) {
    super(serial);
    this.replySerial = Objects.requireNonNull(replySerial);
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param signature   optional; the signature of the message body
   * @param payload     optional; the message body
   */
  AbstractReply(
          UInt32 serial,
          UInt32 replySerial,
          Signature signature,
          List<? extends DBusType> payload) {
    super(serial, signature, payload);
    this.replySerial = Objects.requireNonNull(replySerial);
  }

  @Override
  public UInt32 getReplySerial() {
    return replySerial;
  }

  @Override
  public String toString() {
    var s = "AbstractReply{serial='%s', replySerial='%s', sig='%s'}";
    var sig = getSignature().map(Signature::toString).orElse("");
    return String.format(s, getSerial(), replySerial, sig);
  }
}
