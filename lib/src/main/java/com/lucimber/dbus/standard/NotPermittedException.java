package com.lucimber.dbus.standard;

/**
 * Specific DBus exception which gets thrown if a caller is not permitted to perform an action.
 */
public final class NotPermittedException extends Exception {

  private static final String ERROR_NAME = "com.lucimber.DBus1.Error.NotPermitted";

  /**
   * Creates a new instance.
   */
  public NotPermittedException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance.
   *
   * @param cause the optional cause
   */
  public NotPermittedException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }
}
