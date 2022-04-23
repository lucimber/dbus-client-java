package com.lucimber.dbus.standard;

/**
 * Specific DBus exception which gets thrown if a caller tries to perform an action that is not supported.
 */
public final class NotSupportedException extends Exception {

  private static final String ERROR_NAME = "com.lucimber.DBus1.Error.NotSupported";

  /**
   * Creates a new instance.
   */
  public NotSupportedException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance.
   *
   * @param cause the optional cause
   */
  public NotSupportedException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }
}
