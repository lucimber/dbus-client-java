package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;

/**
 * An outbound message is a message that should be sent from this service to another service
 * on the same bus that this service is connected to.
 */
public interface OutboundMessage extends Message {

    /**
     * Gets the unique name of the destination.
     *
     * @return a {@link DBusString}
     */
    DBusString getDestination();

    /**
     * Sets the unique name of the destination.
     *
     * @param destination a {@link DBusString}
     */
    void setDestination(DBusString destination);
}
