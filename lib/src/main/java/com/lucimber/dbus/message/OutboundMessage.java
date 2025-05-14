/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.protocol.types.DBusString;
import java.util.Optional;

/**
 * An outbound message is a message that should be sent from this service to another service
 * on the same bus that this service is connected to.
 */
public interface OutboundMessage extends Message {

  /**
   * Gets the unique name of the destination.
   *
   * @return an {@link Optional} of {@link DBusString}
   */
  Optional<DBusString> getDestination();

  /**
   * Sets the unique name of the destination.
   *
   * @param destination a {@link DBusString}
   */
  void setDestination(DBusString destination);
}
