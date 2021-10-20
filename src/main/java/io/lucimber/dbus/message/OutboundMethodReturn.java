package io.lucimber.dbus.message;

import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.UInt32;

import java.util.Objects;

public final class OutboundMethodReturn extends AbstractReply implements OutboundReply {

    private DBusString destination;

    /**
     * Constructs a new instance with mandatory arguments.
     *
     * @param destination a {@link DBusString}
     * @param serial      an {@link UInt32}
     * @param replySerial an {@link UInt32}
     */
    public OutboundMethodReturn(final DBusString destination, final UInt32 serial,
                                final UInt32 replySerial) {
        super(serial, replySerial);
        this.destination = Objects.requireNonNull(destination);
    }

    @Override
    public DBusString getDestination() {
        return destination;
    }

    @Override
    public void setDestination(final DBusString destination) {
        this.destination = Objects.requireNonNull(destination);
    }

    @Override
    public String toString() {
        final String s = "OutboundMethodReturn{destination='%s', serial=%s, replySerial=%s, signature=%s}";
        return String.format(s, destination, getSerial(), getReplySerial(), getSignature());
    }
}
