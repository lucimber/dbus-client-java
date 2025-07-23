/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.standard;

import com.lucimber.dbus.type.DBusString;

/**
 * The {@literal org.freedesktop.DBus.Introspectable} interface provides introspection data for
 * D-Bus objects. This allows clients to discover the interfaces, methods, signals, and properties
 * supported by an object at runtime.
 *
 * <p>The introspection data is returned as an XML document following the D-Bus introspection
 * format. This is the standard mechanism for service discovery in D-Bus.
 *
 * <p>Example introspection data:
 *
 * <pre>{@code
 * <node>
 *   <interface name="org.example.MyInterface">
 *     <method name="MyMethod">
 *       <arg name="input" type="s" direction="in"/>
 *       <arg name="output" type="s" direction="out"/>
 *     </method>
 *     <signal name="MySignal">
 *       <arg name="value" type="i"/>
 *     </signal>
 *     <property name="MyProperty" type="s" access="readwrite"/>
 *   </interface>
 * </node>
 * }</pre>
 *
 * @see <a
 *     href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces-introspectable">D-Bus
 *     Specification - Introspectable</a>
 */
public interface Introspectable {
    /**
     * Returns the introspection data for the object in XML format.
     *
     * <p>The returned XML document describes all interfaces implemented by the object, including
     * their methods, signals, and properties. Child nodes in the object hierarchy are also listed.
     *
     * @return A {@link DBusString} containing the XML introspection data
     */
    DBusString introspect();
}
