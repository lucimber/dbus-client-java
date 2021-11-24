package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps a {@link Double} to its D-Bus equivalent of DOUBLE.
 */
public final class DBusDouble extends Number implements Comparable<DBusDouble>, DBusBasicType {

  private final double delegate;

  private DBusDouble(final double delegate) {
    this.delegate = delegate;
  }

  public static DBusDouble valueOf(final double value) {
    return new DBusDouble(value);
  }

  @Override
  public Type getType() {
    return Type.DOUBLE;
  }

  @Override
  public Double getDelegate() {
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
    final DBusDouble that = (DBusDouble) o;
    return Double.compare(that.delegate, delegate) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return Double.toString(delegate);
  }

  @Override
  public int intValue() {
    return (int) delegate;
  }

  @Override
  public long longValue() {
    return (long) delegate;
  }

  @Override
  public float floatValue() {
    return (float) delegate;
  }

  @Override
  public double doubleValue() {
    return delegate;
  }

  @Override
  public int compareTo(final DBusDouble o) {
    return Double.compare(delegate, o.delegate);
  }
}
