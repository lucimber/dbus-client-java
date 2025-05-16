/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;


import com.lucimber.dbus.protocol.types.BetterDBusObjectPath;
import com.lucimber.dbus.protocol.types.BetterDBusString;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Objects;

/**
 * An inbound method call.
 *
 * @since 1.0
 */
public final class InboundMethodCall extends AbstractMethodCall implements InboundMessage {

  private BetterDBusString sender;
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
  public InboundMethodCall(BetterDBusUInt32 serial, BetterDBusString sender, BetterDBusObjectPath objectPath,
                           BetterDBusString name, boolean replyExpected) {
    super(serial, objectPath, name);
    this.sender = Objects.requireNonNull(sender);
    this.replyExpected = replyExpected;
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
   * Defines if a reply to this method call is expected from the sender or not.
   *
   * @param replyExpected Set to {@code TRUE} if reply is expected.
   */
  public void setReplyExpected(boolean replyExpected) {
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

  @Override
  public String toString() {
    var s = "InboundMethodCall{sender='%s', serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
    return String.format(s, sender, getSerial(), getObjectPath(), getInterfaceName(), getName(), getSignature());
  }
}
