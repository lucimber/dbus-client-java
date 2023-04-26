package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps a {@link Boolean} to its D-Bus equivalent of BOOLEAN.
 */
public final class DBusBoolean implements DBusBasicType {

  private final boolean delegate;

  private DBusBoolean(final boolean delegate) {
    this.delegate = delegate;
  }

  /**
   * Constructs a new D-Bus boolean from its Java counterpart.
   *
   * @param value the boolean value
   * @return a new instance
   */
  public static DBusBoolean valueOf(final boolean value) {
    return new DBusBoolean(value);
  }

  @Override
  public Type getType() {
    return Type.BOOLEAN;
  }

  @Override
  public Boolean getDelegate() {
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
    final DBusBoolean that = (DBusBoolean) o;
    return delegate == that.delegate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return Boolean.toString(delegate);
  }
}
