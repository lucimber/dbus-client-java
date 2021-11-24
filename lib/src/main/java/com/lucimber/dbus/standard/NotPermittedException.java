package com.lucimber.dbus.standard;

/**
 * Specific runtime exception which gets thrown if a caller is not permitted to perform an action.
 */
public final class NotPermittedException extends DbusRuntimeException {

  private static final String ERROR_NAME = "io.lucimber.DBus1.Error.NotPermitted";

  /**
   * Creates a new instance.
   */
  public NotPermittedException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance.
   *
   * @param message the optional detail message
   */
  public NotPermittedException(final String message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance.
   *
   * @param message the optional detail message
   * @param cause   the optional cause
   */
  public NotPermittedException(final String message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
