package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.UInt32;
import java.util.Objects;

/**
 * An outbound error message.
 */
public final class OutboundError extends AbstractReply implements OutboundReply {

  private DBusString destination;
  private DBusString name;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param destination the destination of this error
   * @param name        the name of this error
   */
  public OutboundError(final UInt32 serial, final UInt32 replySerial,
                       final DBusString destination, final DBusString name) {
    super(serial, replySerial);
    this.destination = Objects.requireNonNull(destination);
    this.name = Objects.requireNonNull(name);
  }

  @Override
  public DBusString getDestination() {
    return destination;
  }

  @Override
  public void setDestination(final DBusString destination) {
    this.destination = Objects.requireNonNull(destination);
  }

  /**
   * Returns the name of this error.
   *
   * @return a {@link DBusString}
   */
  public DBusString getName() {
    return name;
  }

  /**
   * Sets the name of this error.
   *
   * @param name a {@link DBusString}
   */
  public void setName(final DBusString name) {
    this.name = Objects.requireNonNull(name);
  }

  @Override
  public String toString() {
    final String s = "OutboundError{destination='%s', serial=%s, replySerial=%s, name='%s', signature=%s}";
    return String.format(s, destination, getSerial(), getReplySerial(), getName(), getSignature());
  }
}
