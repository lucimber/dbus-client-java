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
 * Access to the requested operation is not permitted.
 * However, it might be available after interactive authentication.
 * This is usually returned by method calls supporting a framework for additional interactive authorization.
 */
public final class InteractiveAuthorizationRequiredException extends AbstractException {

  private static final DBusString ERROR_NAME =
          DBusString.valueOf("org.freedesktop.DBus.Error.InteractiveAuthorizationRequired");

  /**
   * Creates a new instance.
   */
  public InteractiveAuthorizationRequiredException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public InteractiveAuthorizationRequiredException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public InteractiveAuthorizationRequiredException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public InteractiveAuthorizationRequiredException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
