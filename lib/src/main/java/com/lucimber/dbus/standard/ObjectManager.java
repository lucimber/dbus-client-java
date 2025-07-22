/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.standard;

import com.lucimber.dbus.type.DBusDict;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusVariant;

/**
 * The {@literal org.freedesktop.DBus.ObjectManager} interface provides a standardized way
 * to enumerate all objects below a certain path in the object hierarchy, along with
 * their interfaces and properties.
 * 
 * <p>The ObjectManager interface is particularly useful for:</p>
 * <ul>
 *   <li>Service discovery and enumeration</li>
 *   <li>Dynamic object tree exploration</li>
 *   <li>Efficient bulk queries of object hierarchies</li>
 *   <li>Monitoring object lifecycle through signals</li>
 * </ul>
 * 
 * <p>This interface works in conjunction with the standard Introspectable and Properties
 * interfaces to provide comprehensive object discovery capabilities.</p>
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#standard-interfaces-objectmanager">D-Bus Specification - ObjectManager</a>
 */
public interface ObjectManager {
  /**
   * Gets all managed objects below this object path.
   * 
   * <p>Returns a dictionary where:</p>
   * <ul>
   *   <li>Keys are object paths of managed objects</li>
   *   <li>Values are dictionaries of interfaces implemented by each object</li>
   *   <li>Interface dictionaries contain property names and their values</li>
   * </ul>
   * 
   * <p>Example return structure:</p>
   * <pre>{@code
   * {
   *   "/org/example/Object1": {
   *     "org.example.Interface1": {
   *       "Property1": <variant:string:"value1">,
   *       "Property2": <variant:int32:42>
   *     },
   *     "org.example.Interface2": {
   *       "PropertyA": <variant:boolean:true>
   *     }
   *   },
   *   "/org/example/Object2": {
   *     "org.example.Interface1": {
   *       "Property1": <variant:string:"value2">
   *     }
   *   }
   * }
   * }</pre>
   * 
   * <p>This method provides a snapshot of the entire managed object tree at the
   * time of the call. For monitoring changes, use the InterfacesAdded and
   * InterfacesRemoved signals.</p>
   *
   * @return A nested dictionary structure containing all managed objects,
   *         their interfaces, and properties
   */
  DBusDict<DBusObjectPath, DBusDict<DBusString, DBusDict<DBusString, DBusVariant>>> getManagedObjects();
}