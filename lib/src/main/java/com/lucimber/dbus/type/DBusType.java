package com.lucimber.dbus.type;

/**
 * Common interface of all D-Bus data types.
 */
public interface DBusType {

  /**
   * Gets the type of this implementation.
   *
   * @return a {@link Type}
   */
  Type getType();
}
