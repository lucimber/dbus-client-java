package com.lucimber.dbus.standard;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Dict;
import com.lucimber.dbus.type.Variant;
import java.util.Optional;

/**
 * The {@literal org.freedesktop.DBus.Properties} interface provides methods to expose properties or attributes
 * of objects. It is conventional to give D-Bus properties names consisting of capitalized words without
 * punctuation ("CamelCase"), like member names. Strictly speaking, D-Bus property names are not required
 * to follow the same naming restrictions as member names, but D-Bus property names that would not be
 * valid member names (in particular, GObject-style dash-separated property names) can cause interoperability
 * problems and should be avoided.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces"
 * target="_top">D-Bus Specification (Standard Interfaces)</a>
 */
public interface Properties {
  /**
   * Gets the value of a property.
   *
   * @param interfaceName the name of the interface
   * @param propertyName  the name of the property
   * @return An {@link Optional} of {@link Variant}.
   * @throws NotSupportedException If the interface or property is not supported by this object.
   * @throws NotPermittedException If the property is not accessible to the caller.
   */
  Optional<Variant> getProperty(DBusString interfaceName, DBusString propertyName)
          throws NotSupportedException, NotPermittedException;

  /**
   * Sets the value of a property.
   *
   * @param interfaceName the name of the interface
   * @param propertyName  the name of the property
   * @param value         the value that should be assigned
   * @throws NotSupportedException If the interface or property is not supported by this object.
   * @throws NotPermittedException If the property is read-only or not accessible to the caller.
   */
  void setProperty(DBusString interfaceName, DBusString propertyName, Variant value)
          throws NotSupportedException, NotPermittedException;

  /**
   * Gets all properties and their values.
   *
   * @param interfaceName the name of the interface
   * @return A {@link Dict} of {@link DBusString} and {@link Variant}.
   * @throws NotSupportedException If the interface is not supported by this object.
   */
  Dict<DBusString, Variant> getProperties(DBusString interfaceName) throws NotSupportedException;
}
