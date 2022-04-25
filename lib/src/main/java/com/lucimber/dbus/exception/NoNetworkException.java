package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/**
 * No network available to execute requested network operation on.
 */
public final class NoNetworkException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.NoNetwork");

  /**
   * Creates a new instance.
   */
  public NoNetworkException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public NoNetworkException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public NoNetworkException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public NoNetworkException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
