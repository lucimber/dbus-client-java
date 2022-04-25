package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/**
 * The requested property does not exist in the selected interface.
 */
public final class UnknownPropertyException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.UnknownProperty");

  /**
   * Creates a new instance.
   */
  public UnknownPropertyException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public UnknownPropertyException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public UnknownPropertyException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public UnknownPropertyException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
