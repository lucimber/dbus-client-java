/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/**
 * The specified network address is already being listened on.
 */
public final class AddressInUseException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.AddressInUse");

  /**
   * Creates a new instance.
   */
  public AddressInUseException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public AddressInUseException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public AddressInUseException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public AddressInUseException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
