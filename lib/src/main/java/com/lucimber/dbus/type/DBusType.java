/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

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
