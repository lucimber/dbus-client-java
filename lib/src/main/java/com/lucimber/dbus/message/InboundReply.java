/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.protocol.types.UInt32;

/**
 * An inbound reply to a previously send message.
 */
public interface InboundReply extends Message, InboundMessage {

  /**
   * Gets the reply-serial of this inbound reply.
   *
   * @return an {@link UInt32}
   */
  UInt32 getReplySerial();

  /**
   * Sets the reply-serial of this inbound reply.
   *
   * @param replySerial an {@link UInt32}
   */
  void setReplySerial(UInt32 replySerial);
}
