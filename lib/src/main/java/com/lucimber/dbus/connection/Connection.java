/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.type.UInt32;

/**
 * Virtual channel that mediates the message between processes.
 * There are two type of buses. System Bus and Session Bus.
 * System Bus is a global bus that is used by both system and user processes.
 * A Session Bus is specific to a user login session.
 * An application can create a new Session Bus for communicating with other processes in that session.
 */
public interface Connection {

  /**
   * Returns the assigned {@link Pipeline}.
   *
   * @return the {@link Pipeline}
   */
  Pipeline getPipeline();

  /**
   * Gets a serial number that can be used for an {@link OutboundMessage}.
   * The serial number is guaranteed to be unique to this {@link Connection}.
   *
   * @return a serial number
   */
  UInt32 getNextSerial();

  /**
   * Writes an {@link OutboundMessage} on this {@link Connection}.
   *
   * @param message the {@link OutboundMessage}
   */
  void writeOutboundMessage(OutboundMessage message);
}
