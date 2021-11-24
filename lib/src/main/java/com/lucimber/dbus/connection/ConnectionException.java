package com.lucimber.dbus.connection;

/**
 * A checked exception that gets thrown by a connection factory if something went wrong.
 */
public final class ConnectionException extends Exception {

  public ConnectionException(final String message) {
    super(message);
  }

  public ConnectionException(final Throwable cause) {
    super(cause);
  }

  public ConnectionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
