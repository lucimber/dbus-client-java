/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import java.util.Objects;
import java.util.Optional;

/**
 * An outbound method call.
 */
public final class OutboundMethodCall extends AbstractMethodCall implements OutboundMessage {

  private DBusString destination;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param destination the destination of this method call
   * @param objectPath  the object path
   * @param name        the name of the method
   */
  public OutboundMethodCall(final UInt32 serial, final DBusString destination,
                            final ObjectPath objectPath, final DBusString name) {
    super(serial, objectPath, name);
    this.destination = Objects.requireNonNull(destination);
  }

  @Override
  public String toString() {
    final String s = "OutboundMethodCall{destination='%s', serial=%s, path=%s,"
            + " interface='%s', member='%s', signature=%s}";
    return String.format(s, destination, getSerial(), getObjectPath(), getInterfaceName(), getName(),
            getSignature());
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.of(destination);
  }

  @Override
  public void setDestination(final DBusString destination) {
    this.destination = Objects.requireNonNull(destination);
  }
}
