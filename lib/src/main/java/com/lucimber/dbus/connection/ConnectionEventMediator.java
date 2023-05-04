/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection;

/**
 * Mediates between interested receivers of the connection events
 * that are attached to the same {@link Pipeline}.
 */
public interface ConnectionEventMediator {
  /**
   * Passes the event of the activation of the D-Bus connection upwards the {@link Pipeline}.
   */
  void passConnectionActiveEvent();

  /**
   * Passes the event of the inactivation of the D-Bus connection upwards the {@link Pipeline}.
   */
  void passConnectionInactiveEvent();
}
