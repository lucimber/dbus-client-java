/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.protocol.types.DBusString;

/**
 * The contacted bus service is unknown and cannot be activated.
 */
public final class ServiceUnknownException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.ServiceUnknown");

  /**
   * Creates a new instance.
   */
  public ServiceUnknownException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public ServiceUnknownException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public ServiceUnknownException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public ServiceUnknownException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
