/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.util;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Provides common methods necessary for logging and standard logging markers for categorizing log output.
 *
 * <p>The class provides the following types of markers: High-level categories like SASL, DBUS, TRANSPORT, MARSHALLING, etc.
 *
 * <p>Example usage:
 * <pre>{@code
 * LOGGER.info(LoggerUtils.SASL, "Authentication completed successfully");
 * LOGGER.debug(LoggerUtils.DBUS, "Sending Hello method call");
 * LOGGER.trace(LoggerUtils.MARSHALLING, "Encoding message body");
 * }</pre>
 */
public final class LoggerUtils {

  /**
   * Authentication processes
   */
  public static final Marker SASL = MarkerFactory.getMarker("SASL");
  /**
   * D-Bus protocol operations
   */
  public static final Marker DBUS = MarkerFactory.getMarker("DBUS");
  /**
   * Network transport operations
   */
  public static final Marker TRANSPORT = MarkerFactory.getMarker("TRANSPORT");
  /**
   * Message marshalling and unmarshalling operations
   */
  public static final Marker MARSHALLING = MarkerFactory.getMarker("MARSHALLING");
  /**
   * Handler pipeline management
   */
  public static final Marker HANDLER_LIFECYCLE = MarkerFactory.getMarker("HANDLER_LIFECYCLE");
  /**
   * Connection state management
   */
  public static final Marker CONNECTION = MarkerFactory.getMarker("CONNECTION");
  /**
   * Health monitoring
   */
  public static final Marker HEALTH = MarkerFactory.getMarker("HEALTH");

  private LoggerUtils() {
    // Utility class
  }
}
