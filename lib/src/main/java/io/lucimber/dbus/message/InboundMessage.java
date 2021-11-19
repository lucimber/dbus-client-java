package io.lucimber.dbus.message;

import io.lucimber.dbus.type.DBusString;

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
