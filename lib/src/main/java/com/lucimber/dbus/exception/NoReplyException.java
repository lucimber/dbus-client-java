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
 * A message did not receive a reply. This error is usually generated after a timeout.
 */
public final class NoReplyException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.NoReply");

  /**
   * Creates a new instance.
   */
  public NoReplyException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public NoReplyException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public NoReplyException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public NoReplyException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
