/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection;

/**
 * Mediates between interested receivers of user events
 * that are attached to the same {@link Pipeline}.
 */
public interface UserEventMediator {
  /**
   * Passes a user event further up the {@link Pipeline}.
   *
   * @param event the user event
   */
  void passUserEvent(Object event);

}
