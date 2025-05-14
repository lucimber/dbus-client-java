/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.protocol.types.UInt32;

/**
 * An outbound reply to a previously received message.
 */
public interface OutboundReply extends Message, OutboundMessage {

  /**
   * Gets the serial number of the message this message is a reply to.
   *
   * @return an {@link UInt32}
   */
  UInt32 getReplySerial();

  /**
   * Sets the serial number of the message this message is a reply to.
   *
   * @param replySerial an {@link UInt32}
   */
  void setReplySerial(UInt32 replySerial);
}
