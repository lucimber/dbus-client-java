/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import java.util.Objects;

/**
 * An inbound method call.
 */
public final class InboundMethodCall extends AbstractMethodCall implements InboundMessage {

  private DBusString sender;
  private boolean replyExpected;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial        the serial number
   * @param sender        the sender of this method call
   * @param objectPath    the object path
   * @param name          the name of the method
   * @param replyExpected states if reply is expected
   */
  public InboundMethodCall(final UInt32 serial, final DBusString sender, final ObjectPath objectPath,
                           final DBusString name, final boolean replyExpected) {
    super(serial, objectPath, name);
    this.sender = Objects.requireNonNull(sender);
    this.replyExpected = replyExpected;
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
    final String s = "InboundMethodCall{sender='%s', serial=%s, path=%s"
        + ", interface='%s', member='%s', signature=%s}";
    return String.format(s, sender, getSerial(), getObjectPath(), getInterfaceName(), getName(), getSignature());
  }

  /**
   * Defines if a reply to this method call is expected from the sender or not.
   *
   * @param replyExpected Set to {@code TRUE} if reply is expected.
   */
  public void setReplyExpected(final boolean replyExpected) {
    this.replyExpected = replyExpected;
  }

  /**
   * States if the sender expects a reply to this method call or not.
   *
   * @return {@code TRUE} if reply is expected, {@code FALSE} otherwise.
   */
  public boolean isReplyExpected() {
    return replyExpected;
  }
}
