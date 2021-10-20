package io.lucimber.dbus.message;

import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.ObjectPath;
import io.lucimber.dbus.type.UInt32;

import java.util.Objects;

public final class OutboundMethodCall extends AbstractMethodCall implements OutboundMessage {

    private DBusString destination;

    /**
     * Constructs a new instance with mandatory parameters.
     *
     * @param serial      an {@link UInt32}
     * @param destination a {@link DBusString}
     * @param objectPath  an {@link ObjectPath}
     * @param name        a {@link DBusString}
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
    public DBusString getDestination() {
        return destination;
    }

    @Override
    public void setDestination(final DBusString destination) {
        this.destination = Objects.requireNonNull(destination);
    }
}
