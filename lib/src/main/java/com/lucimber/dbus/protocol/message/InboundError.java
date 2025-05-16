/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusString;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Objects;

/**
 * An inbound error message.
 *
 * @since 1.0
 */
public final class InboundError extends AbstractReply implements InboundReply {

  private BetterDBusString sender;
  private BetterDBusString name;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param sender      the sender of this error
   * @param name        the name of the error
   */
  public InboundError(BetterDBusUInt32 serial, BetterDBusUInt32 replySerial,
                      BetterDBusString sender, BetterDBusString name) {
    super(serial, replySerial);
    this.sender = Objects.requireNonNull(sender);
    this.name = Objects.requireNonNull(name);
  }

  @Override
  public BetterDBusString getSender() {
    return sender;
  }

  @Override
  public void setSender(BetterDBusString sender) {
    this.sender = Objects.requireNonNull(sender);
  }

  /**
   * Returns the name of this error.
   *
   * @return a {@link BetterDBusString}
   */
  public BetterDBusString getName() {
    return name;
  }

  /**
   * Sets the name of this error.
   *
   * @param name a {@link BetterDBusString}
   */
  public void setName(BetterDBusString name) {
    this.name = Objects.requireNonNull(name);
  }

  @Override
  public String toString() {
    var s = "InboundError{serial=%s, replySerial=%s, sender='%s', name='%s', signature=%s}";
    return String.format(s, getSerial(), getReplySerial(), sender, name, getSignature());
  }
}
