/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.standard;

import com.lucimber.dbus.exception.AccessDeniedException;
import com.lucimber.dbus.exception.PropertyReadOnlyException;
import com.lucimber.dbus.exception.UnknownInterfaceException;
import com.lucimber.dbus.exception.UnknownPropertyException;
import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusVariant;
import java.util.Optional;

/**
 * The {@literal org.freedesktop.DBus.Properties} interface provides methods to expose properties or
 * attributes of objects. It is conventional to give D-Bus properties names consisting of
 * capitalized words without punctuation ("CamelCase"), like member names. Strictly speaking, D-Bus
 * property names are not required to follow the same naming restrictions as member names, but D-Bus
 * property names that would not be valid member names (in particular, GObject-style dash-separated
 * property names) can cause interoperability problems and should be avoided.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces"
 *     target="_top">D-Bus Specification (Standard Interfaces)</a>
 */
public interface Properties {
    /**
     * Gets the value of a property.
     *
     * @param interfaceName the name of the interface
     * @param propertyName the name of the property
     * @return An {@link Optional} of {@link DBusVariant}.
     * @throws UnknownInterfaceException If the interface is unknown to the implementation.
     * @throws UnknownPropertyException If the property is unknown to the implementation.
     * @throws AccessDeniedException If caller is not allowed to access the property.
     */
    Optional<DBusVariant> getProperty(DBusString interfaceName, DBusString propertyName)
            throws UnknownInterfaceException, UnknownPropertyException, AccessDeniedException;

    /**
     * Sets the value of a property.
     *
     * @param interfaceName the name of the interface
     * @param propertyName the name of the property
     * @param value the value that should be assigned
     * @throws UnknownInterfaceException If the interface is unknown to the implementation.
     * @throws UnknownPropertyException If the property is unknown to the implementation.
     * @throws AccessDeniedException If caller is not allowed to access the property.
     * @throws PropertyReadOnlyException If the property can only be read.
     */
    void setProperty(DBusString interfaceName, DBusString propertyName, DBusVariant value)
            throws UnknownInterfaceException,
                    UnknownPropertyException,
                    AccessDeniedException,
                    PropertyReadOnlyException;

    /**
     * Gets all properties. Properties, to which the caller has no access, are silently omitted from
     * the result array.
     *
     * @param interfaceName the name of the interface
     * @return A {@link DBusDict} of {@link DBusString} and {@link DBusVariant}.
     * @throws UnknownInterfaceException If the interface is unknown to the implementation.
     */
    DBusDict<DBusString, DBusVariant> getProperties(DBusString interfaceName)
            throws UnknownInterfaceException;
}
