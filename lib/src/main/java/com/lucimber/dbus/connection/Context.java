/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

/**
 * The context object that wraps a {@link Handler} and links it into the {@link Pipeline}.
 * It provides methods to interact with and propagate events through the pipeline.
 */
public interface Context extends InboundPropagator, OutboundPropagator, UserEventPropagator {

  /**
   * Returns the name of this context in the pipeline.
   *
   * @return the handler name
   */
  String getName();

  /**
   * Returns the handler instance bound to this context.
   *
   * @return the handler
   */
  Handler getHandler();

  /**
   * Returns the pipeline instance bound to this context.
   *
   * @return the pipeline
   */
  Pipeline getPipeline();

  /**
   * Returns the connection instance bound to this context.
   *
   * @return the connection
   */
  Connection getConnection();

  /**
   * Indicates whether the handler has been removed from the pipeline.
   *
   * @return {@code true} if the handler is removed, {@code false} otherwise.
   */
  boolean isRemoved();
}
