package com.lucimber.dbus.type;

import java.util.List;

/**
 * The D-Bus type system consists of two distinct categories known as basic and container types.
 * The container types are complex types. Their structure is defined by the type code of the content
 * and their enclosing type code(s).
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#container-types">
 * D-Bus Specification: Container types</a>
 */
public interface DBusContainerType extends DBusType {
  /**
   * Gets the signature of this container type.
   * The returned signature describes the container and its content.
   *
   * @return a {@link Signature}
   */
  Signature getSignature();

  /**
   * Gets the wrapped value of this container type.
   * The D-Bus type system is made of ASCII characters representing the value's type.
   * Each D-Bus data type is mapped in this framework by its corresponding class.
   * This method enables the access to the Java's data type - the delegate.
   * If this container is for example a {@link DBusArray}, the returned object will be a {@link List}.
   *
   * @return an {@link Object}
   */
  Object getDelegate();
}
