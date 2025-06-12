package com.lucimber.dbus.message;

import com.lucimber.dbus.type.UInt32;

/**
 * A reply to a previously send message.
 *
 * @since 2.0
 */
public interface Reply extends Message {

  /**
   * Gets the serial number of the message this message is a reply to.
   *
   * @return The serial number as an {@link UInt32}.
   */
  UInt32 getReplySerial();
}
