package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/**
 * One or more invalid arguments have been passed.
 */
public final class InvalidArgsException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.InvalidArgs");

  /**
   * Creates a new instance.
   */
  public InvalidArgsException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public InvalidArgsException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public InvalidArgsException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public InvalidArgsException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
