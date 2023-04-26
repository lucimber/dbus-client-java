package com.lucimber.dbus.connection;

/**
 * A checked exception that gets thrown by a connection factory if something went wrong.
 */
public final class ConnectionException extends Exception {

  /**
   * Constructs a new exception with the specified detail message and cause.
   * @param message the detail message
   * @param cause the cause
   */
  public ConnectionException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
