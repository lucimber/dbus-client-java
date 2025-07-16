/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/**
 * The specified message signature is not valid.
 */
public final class InvalidSignatureException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.InvalidSignature");

  /**
   * Creates a new instance.
   */
  public InvalidSignatureException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public InvalidSignatureException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public InvalidSignatureException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public InvalidSignatureException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
