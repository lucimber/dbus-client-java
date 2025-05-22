/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A {@link RuntimeException} that gets thrown by {@link ObjectPath},
 * if a marshalled {@link ObjectPath} cannot be parsed.
 */
public class ObjectPathException extends RuntimeException {
  /**
   * Creates a new instance with a message.
   *
   * @param message the associated message
   */
  public ObjectPathException(final String message) {
    super(message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the associated cause
   */
  public ObjectPathException(final Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new instance with a message and a cause.
   *
   * @param message the associated message
   * @param cause   the associated cause
   */
  public ObjectPathException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
