package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;

import java.util.Objects;

public final class InboundSignal extends AbstractSignal implements InboundMessage {

    private DBusString sender;

    public InboundSignal(final UInt32 serial, final DBusString sender, final ObjectPath objectPath,
                         final DBusString interfaceName, final DBusString name) {
        super(serial, objectPath, interfaceName, name);
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
        final String s = "InboundSignal{sender='%s', serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
        return String.format(s, sender, getSerial(), getObjectPath(), getInterfaceName(), getName(), getSignature());
    }
}
