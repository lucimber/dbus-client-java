package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import java.util.Optional;

/**
 * An outbound signal message.
 */
public final class OutboundSignal extends AbstractSignal implements OutboundMessage {

  private DBusString destination;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial        the serial number
   * @param objectPath    the object path
   * @param interfaceName the name of the D-Bus interface
   * @param name          the name of this signal
   */
  public OutboundSignal(final UInt32 serial, final ObjectPath objectPath, final DBusString interfaceName,
                        final DBusString name) {
    super(serial, objectPath, interfaceName, name);
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(destination);
  }

  @Override
  public void setDestination(final DBusString destination) {
    this.destination = destination;
  }

  @Override
  public String toString() {
    final String s = "OutboundSignal{destination='%s', serial=%s, path=%s, interface='%s', member='%s',"
        + " signature=%s}";
    return String.format(s, destination, getSerial(), getObjectPath(), getInterfaceName(), getName(),
        getSignature());
  }
}
