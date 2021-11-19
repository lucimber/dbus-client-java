package io.lucimber.dbus.type;

/**
 * The D-Bus type system consists of two distinct categories known as basic and container types.
 * The basic types are the simplest types and their structure is entirely defined by their 1-character type code.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#basic-types">
 * D-Bus Specification: Basic types</a>
 */
public interface DBusBasicType extends DBusType {
    /**
     * Gets the wrapped value of this basic type.
     * The D-Bus type system is made of ASCII characters representing the value's type.
     * Each D-Bus data type is mapped in this framework by its corresponding class.
     * This method enables the access to the Java's data type - the delegate.
     * If this basic type is for example a {@link DBusBoolean}, the returned object will be a {@link Boolean}.
     *
     * @return an {@link Object}
     */
    Object getDelegate();
}
