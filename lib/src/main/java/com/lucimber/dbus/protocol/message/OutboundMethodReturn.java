/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusString;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Objects;
import java.util.Optional;

/**
 * An outbound method return.
 *
 * @since 1.0
 */
public final class OutboundMethodReturn extends AbstractReply implements OutboundReply {

  private BetterDBusString destination;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param destination the destination of this method return
   * @param serial      the serial number
   * @param replySerial the reply serial number
   */
  public OutboundMethodReturn(BetterDBusString destination, BetterDBusUInt32 serial,
                              BetterDBusUInt32 replySerial) {
    super(serial, replySerial);
    this.destination = Objects.requireNonNull(destination);
  }

  @Override
  public Optional<BetterDBusString> getDestination() {
    return Optional.of(destination);
  }

  @Override
  public void setDestination(BetterDBusString destination) {
    this.destination = Objects.requireNonNull(destination);
  }

  @Override
  public String toString() {
    var s = "OutboundMethodReturn{destination='%s', serial=%s, replySerial=%s, signature=%s}";
    return String.format(s, destination, getSerial(), getReplySerial(), getSignature());
  }
}
