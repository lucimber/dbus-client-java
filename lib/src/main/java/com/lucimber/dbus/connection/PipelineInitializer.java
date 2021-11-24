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
