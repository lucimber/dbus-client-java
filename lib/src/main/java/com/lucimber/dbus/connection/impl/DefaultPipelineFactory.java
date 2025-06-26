/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.impl;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.connection.PipelineFactory;

/**
 * A default implementation of the pipeline factory interface.
 *
 * @see PipelineFactory
 */
public final class DefaultPipelineFactory implements PipelineFactory {
  @Override
  public Pipeline create(Connection connection) {
    return new DefaultPipeline(connection);
  }
}
