package io.lucimber.dbus.message;

import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.ObjectPath;
import io.lucimber.dbus.type.UInt32;

import java.util.Objects;

abstract class AbstractSignal extends AbstractMessage {

    private ObjectPath objectPath;
    private DBusString name;
    private DBusString interfaceName;

    AbstractSignal(final UInt32 serial, final ObjectPath objectPath,
                   final DBusString interfaceName, final DBusString name) {
        super(serial);
        this.objectPath = Objects.requireNonNull(objectPath);
        this.interfaceName = Objects.requireNonNull(interfaceName);
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Gets the object path of this signal.
     *
     * @return The path as an {@link ObjectPath}.
     */
    public ObjectPath getObjectPath() {
        return objectPath;
    }

    /**
     * Sets the object path of this signal.
     *
     * @param objectPath an {@link ObjectPath}
     */
    public void setObjectPath(final ObjectPath objectPath) {
        this.objectPath = Objects.requireNonNull(objectPath);
    }

    /**
     * Gets the name of this signal.
     *
     * @return a {@link DBusString}
     */
    public DBusString getName() {
        return name;
    }

    /**
     * Sets the name of this signal.
     *
     * @param name a {@link DBusString}
     */
    public void setName(final DBusString name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * Gets the name of the interface, to which this signal belongs.
     *
     * @return a {@link DBusString}
     */
    public DBusString getInterfaceName() {
        return interfaceName;
    }

    /**
     * Sets the name of the interface, to which this signal belongs.
     *
     * @param interfaceName a {@link DBusString}
     */
    public void setInterfaceName(final DBusString interfaceName) {
        this.interfaceName = Objects.requireNonNull(interfaceName);
    }

    @Override
    public String toString() {
        final String s = "AbstractSignal{serial=%s, path=%s, interface='%s', member='%s', signature=%s}";
        return String.format(s, getSerial(), objectPath, interfaceName, name, getSignature());
    }
}
