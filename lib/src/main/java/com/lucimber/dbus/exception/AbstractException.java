package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;
import java.util.Objects;

public abstract class AbstractException extends Exception {
  private final DBusString errorName;

  public AbstractException(final DBusString errorName) {
    this.errorName = Objects.requireNonNull(errorName, "error name");
  }

  public AbstractException(final DBusString errorName, final DBusString message) {
    super(message.toString());
    this.errorName = Objects.requireNonNull(errorName, "error name");
  }

  public AbstractException(final DBusString errorName, final Throwable cause) {
    super(cause);
    this.errorName = Objects.requireNonNull(errorName, "error name");
  }

  public AbstractException(final DBusString errorName, final DBusString message, final Throwable cause) {
    super(message.toString(), cause);
    this.errorName = Objects.requireNonNull(errorName, "error name");
  }

  public DBusString getErrorName() {
    return errorName;
  }
}
