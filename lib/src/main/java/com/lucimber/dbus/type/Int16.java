package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps a {@link Short} to its D-Bus equivalent of INT16.
 */
public final class Int16 extends Number implements Comparable<Int16>, DBusBasicType {

  private final short delegate;

  private Int16(final short delegate) {
    this.delegate = delegate;
  }

  public static Int16 valueOf(final short value) {
    return new Int16(value);
  }

  @Override
  public Type getType() {
    return Type.INT16;
  }

  @Override
  public Short getDelegate() {
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
    final Int16 int16 = (Int16) o;
    return delegate == int16.delegate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return Short.toString(delegate);
  }

  @Override
  public int intValue() {
    return delegate;
  }

  @Override
  public long longValue() {
    return delegate;
  }

  @Override
  public float floatValue() {
    return delegate;
  }

  @Override
  public double doubleValue() {
    return delegate;
  }

  @Override
  public int compareTo(final Int16 o) {
    return Short.toUnsignedInt(delegate) - Short.toUnsignedInt(o.delegate);
  }
}
