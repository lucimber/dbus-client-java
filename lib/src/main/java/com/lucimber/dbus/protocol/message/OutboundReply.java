/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

/**
 * An outbound reply to a previously received message.
 *
 * @since 1.0
 */
public interface OutboundReply extends Message, OutboundMessage {

  /**
   * Gets the serial number of the message this message is a reply to.
   *
   * @return an {@link BetterDBusUInt32}
   */
  BetterDBusUInt32 getReplySerial();

  /**
   * Sets the serial number of the message this message is a reply to.
   *
   * @param replySerial an {@link BetterDBusUInt32}
   */
  void setReplySerial(BetterDBusUInt32 replySerial);
}
