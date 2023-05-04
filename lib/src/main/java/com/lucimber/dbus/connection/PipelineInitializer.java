/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection;

/**
 * A component that initializes a pipeline.
 */
public interface PipelineInitializer {
  /**
   * Initializes a pipeline with custom components or settings
   * before it gets used by other components.
   *
   * @param pipeline a (new) pipeline
   */
  void initiate(Pipeline pipeline);
}
