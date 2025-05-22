/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.protocol.types.DBusString;

/**
 * The specified match rule is invalid.
 */
public final class MatchRuleInvalidException extends AbstractException {

  private static final DBusString ERROR_NAME = DBusString.valueOf("org.freedesktop.DBus.Error.MatchRuleInvalid");

  /**
   * Creates a new instance.
   */
  public MatchRuleInvalidException() {
    super(ERROR_NAME);
  }

  /**
   * Creates a new instance with a message.
   *
   * @param message the message
   */
  public MatchRuleInvalidException(final DBusString message) {
    super(ERROR_NAME, message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the cause
   */
  public MatchRuleInvalidException(final Throwable cause) {
    super(ERROR_NAME, cause);
  }

  /**
   * Creates a new instance with a message and cause.
   *
   * @param message the message
   * @param cause   the cause
   */
  public MatchRuleInvalidException(final DBusString message, final Throwable cause) {
    super(ERROR_NAME, message, cause);
  }
}
