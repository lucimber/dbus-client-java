/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.UInt32;
import java.util.Objects;

abstract class AbstractReply extends AbstractMessage {

  private UInt32 replySerial;

  AbstractReply(final UInt32 serial, final UInt32 replySerial) {
    super(serial);
    this.replySerial = Objects.requireNonNull(replySerial);
  }

  /**
   * Gets the serial number of the message this message is a reply to.
   *
   * @return The serial number as an {@link UInt32}.
   */
  public UInt32 getReplySerial() {
    return replySerial;
  }

  /**
   * Sets the serial number of the message this message is a reply to.
   *
   * @param replySerial The serial number as an {@link UInt32}.
   */
  public void setReplySerial(final UInt32 replySerial) {
    this.replySerial = Objects.requireNonNull(replySerial);
  }

  @Override
  public String toString() {
    final String s = "AbstractReply{serial=%s, replySerial=%s, signature=%s}";
    return String.format(s, getSerial(), replySerial, getSignature());
  }
}
