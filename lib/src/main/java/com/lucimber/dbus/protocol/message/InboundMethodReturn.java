/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusString;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Objects;

/**
 * An inbound method return.
 *
 * @since 1.0
 */
public final class InboundMethodReturn extends AbstractReply implements InboundReply {

  private BetterDBusString sender;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param sender      the sender of this method return
   */
  public InboundMethodReturn(BetterDBusUInt32 serial, BetterDBusUInt32 replySerial,
                             BetterDBusString sender) {
    super(serial, replySerial);
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public BetterDBusString getSender() {
    return sender;
  }

  @Override
  public void setSender(BetterDBusString sender) {
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public String toString() {
    var s = "InboundMethodReturn{sender='%s', serial=%s, replySerial=%s, signature=%s}";
    return String.format(s, sender, getSerial(), getReplySerial(), getSignature());
  }
}
