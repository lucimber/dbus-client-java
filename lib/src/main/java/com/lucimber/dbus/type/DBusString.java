package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps a {@link String} to its D-Bus equivalent of STRING.
 */
public final class DBusString implements DBusBasicType {

  private final String delegate;

  private DBusString(final String delegate) {
    this.delegate = delegate;
  }

  /**
   * Constructs a new D-Bus string from its Java counterpart.
   *
   * @param value the string value
   * @return a new instance
   */
  public static DBusString valueOf(final String value) {
    return new DBusString(value);
  }

  @Override
  public Type getType() {
    return Type.STRING;
  }

  @Override
  public String getDelegate() {
    return delegate;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final DBusString that = (DBusString) o;
    return Objects.equals(delegate, that.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return delegate;
  }
}
