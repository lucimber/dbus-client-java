/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection;

/**
 * A factory that creates a pipeline.
 */
public interface PipelineFactory {
  /**
   * Creates a new pipeline instance for a given connection.
   *
   * @param connection a (new) connection
   * @return a new pipeline instance
   */
  Pipeline create(Connection connection);
}
