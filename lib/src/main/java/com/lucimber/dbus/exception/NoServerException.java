package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/**
 * Unable to connect to the specified server.
 */
public final class NoServerException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.NoServer");

  /**
   * Creates a new instance.
   */
  public NoServerException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public NoServerException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public NoServerException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public NoServerException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
