/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.UInt32;
import java.util.Objects;

/**
 * An inbound method return.
 */
public final class InboundMethodReturn extends AbstractReply implements InboundReply {

  private DBusString sender;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param sender      the sender of this method return
   */
  public InboundMethodReturn(final UInt32 serial, final UInt32 replySerial,
                             final DBusString sender) {
    super(serial, replySerial);
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public DBusString getSender() {
    return sender;
  }

  @Override
  public void setSender(final DBusString sender) {
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public String toString() {
    final String s = "InboundMethodReturn{sender='%s', serial=%s, replySerial=%s, signature=%s}";
    return String.format(s, sender, getSerial(), getReplySerial(), getSignature());
  }
}
