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
 * An outbound error message.
 *
 * @since 1.0
 */
public final class OutboundError extends AbstractReply implements OutboundReply {

  private BetterDBusString destination;
  private BetterDBusString name;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param destination the destination of this error
   * @param name        the name of this error
   */
  public OutboundError(BetterDBusUInt32 serial, BetterDBusUInt32 replySerial,
                       BetterDBusString destination, BetterDBusString name) {
    super(serial, replySerial);
    this.destination = Objects.requireNonNull(destination);
    this.name = Objects.requireNonNull(name);
  }

  @Override
  public Optional<BetterDBusString> getDestination() {
    return Optional.of(destination);
  }

  @Override
  public void setDestination(BetterDBusString destination) {
    this.destination = Objects.requireNonNull(destination);
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
    var s = "OutboundError{destination='%s', serial=%s, replySerial=%s, name='%s', signature=%s}";
    return String.format(s, destination, getSerial(), getReplySerial(), getName(), getSignature());
  }
}
