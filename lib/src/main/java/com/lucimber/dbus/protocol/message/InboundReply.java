/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

/**
 * An inbound reply to a previously send message.
 *
 * @since 1.0
 */
public interface InboundReply extends Message, InboundMessage {

  /**
   * Gets the reply-serial of this inbound reply.
   *
   * @return an {@link BetterDBusUInt32}
   */
  BetterDBusUInt32 getReplySerial();

  /**
   * Sets the reply-serial of this inbound reply.
   *
   * @param replySerial an {@link BetterDBusUInt32}
   */
  void setReplySerial(BetterDBusUInt32 replySerial);
}
