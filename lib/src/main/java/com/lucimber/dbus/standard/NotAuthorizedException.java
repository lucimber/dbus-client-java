package com.lucimber.dbus.standard;

/**
 * Specific runtime exception which gets thrown if a caller is not authorized to perform an action.
 */
public final class NotAuthorizedException extends DbusRuntimeException {

  private static final String ERROR_NAME = "io.lucimber.DBus1.Error.NotAuthorized";

  /**
   * Creates a new instance.
   */
  public NotAuthorizedException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance.
   *
   * @param message the optional detail message
   */
  public NotAuthorizedException(final String message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance.
   *
   * @param message the optional detail message
   * @param cause   the optional cause
   */
  public NotAuthorizedException(final String message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
