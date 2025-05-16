/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.UInt32;
import java.util.Objects;
import java.util.Optional;

/**
 * An outbound method return.
 */
public final class OutboundMethodReturn extends AbstractReply implements OutboundReply {

  private DBusString destination;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param destination the destination of this method return
   * @param serial      the serial number
   * @param replySerial the reply serial number
   */
  public OutboundMethodReturn(final DBusString destination, final UInt32 serial,
                              final UInt32 replySerial) {
    super(serial, replySerial);
    this.destination = Objects.requireNonNull(destination);
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.of(destination);
  }

  @Override
  public void setDestination(final DBusString destination) {
    this.destination = Objects.requireNonNull(destination);
  }

  @Override
  public String toString() {
    final String s = "OutboundMethodReturn{destination='%s', serial=%s, replySerial=%s, signature=%s}";
    return String.format(s, destination, getSerial(), getReplySerial(), getSignature());
  }
}
