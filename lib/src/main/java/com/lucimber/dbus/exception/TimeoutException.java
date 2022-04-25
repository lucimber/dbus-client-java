package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/**
 * An operation timed out.
 */
public final class TimeoutException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.Timeout");

  /**
   * Creates a new instance.
   */
  public TimeoutException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public TimeoutException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public TimeoutException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public TimeoutException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
