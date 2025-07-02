/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

/**
 * Defines a component that can propagate user-defined events through a {@link Pipeline}.
 * <p>
 * Typically implemented by {@link Context} instances or internal pipeline elements,
 * this interface allows application-specific events to traverse the pipeline toward the tail.
 *
 * @see Context
 * @see Pipeline
 */
public interface UserEventPropagator {

  /**
   * Propagates a user-defined event to the next handler in the pipeline.
   * <p>
   * If not intercepted, the event will eventually reach the tail handler.
   *
   * @param evt the event object to propagate; must not be {@code null}.
   */
  void propagateUserEvent(Object evt);
}
