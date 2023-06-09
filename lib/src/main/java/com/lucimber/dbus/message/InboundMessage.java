/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;

/**
 * An inbound message is a message that got send from another service to this service
 * on the same bus that this service is connected to.
 */
public interface InboundMessage extends Message {

  /**
   * Gets the sender of this inbound message.
   *
   * @return a {@link DBusString}
   */
  DBusString getSender();

  /**
   * Sets the sender of this inbound message.
   * Must not be {@code NULL}.
   *
   * @param sender a {@link DBusString}
   */
  void setSender(DBusString sender);
}
