package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import java.util.Objects;

/**
 * An inbound method call.
 */
public final class InboundMethodCall extends AbstractMethodCall implements InboundMessage {

  private DBusString sender;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial     the serial number
   * @param sender     the sender of this method call
   * @param objectPath the object path
   * @param name       the name of the method
   */
  public InboundMethodCall(final UInt32 serial, final DBusString sender,
                           final ObjectPath objectPath, final DBusString name) {
    super(serial, objectPath, name);
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public DBusString getSender() {
    return sender;
  }

  @Override
  public void setSender(final DBusString sender) {
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public String toString() {
    final String s = "InboundMethodCall{sender='%s', serial=%s, path=%s"
            + ", interface='%s', member='%s', signature=%s}";
    return String.format(s, sender, getSerial(), getObjectPath(), getInterfaceName(), getName(), getSignature());
  }
}
