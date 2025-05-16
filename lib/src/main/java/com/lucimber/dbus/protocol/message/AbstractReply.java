/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.Objects;

/**
 * @since 1.0
 */
abstract class AbstractReply extends AbstractMessage {

  private BetterDBusUInt32 replySerial;

  AbstractReply(BetterDBusUInt32 serial, BetterDBusUInt32 replySerial) {
    super(serial);
    this.replySerial = Objects.requireNonNull(replySerial);
  }

  /**
   * Gets the serial number of the message this message is a reply to.
   *
   * @return The serial number as an {@link BetterDBusUInt32}.
   */
  public BetterDBusUInt32 getReplySerial() {
    return replySerial;
  }

  /**
   * Sets the serial number of the message this message is a reply to.
   *
   * @param replySerial The serial number as an {@link BetterDBusUInt32}.
   */
  public void setReplySerial(BetterDBusUInt32 replySerial) {
    this.replySerial = Objects.requireNonNull(replySerial);
  }

  @Override
  public String toString() {
    var s = "AbstractReply{serial=%s, replySerial=%s, signature=%s}";
    return String.format(s, getSerial(), replySerial, getSignature());
  }
}
