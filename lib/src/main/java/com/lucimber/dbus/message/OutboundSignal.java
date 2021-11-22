package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;

import java.util.Objects;

public final class OutboundSignal extends AbstractSignal implements OutboundMessage {

    private DBusString destination;

    public OutboundSignal(final UInt32 serial, final DBusString destination,
                          final ObjectPath objectPath, final DBusString interfaceName, final DBusString name) {
        super(serial, objectPath, interfaceName, name);
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
        final String s = "OutboundSignal{destination='%s', serial=%s, path=%s, interface='%s', member='%s',"
                + " signature=%s}";
        return String.format(s, destination, getSerial(), getObjectPath(), getInterfaceName(), getName(),
                getSignature());
    }
}
