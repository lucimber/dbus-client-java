package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps an unsigned {@link Short} to its D-Bus equivalent of UINT16.
 */
public final class UInt16 extends Number implements Comparable<UInt16>, DBusBasicType {

  private final short delegate;

  private UInt16(final short delegate) {
    this.delegate = delegate;
  }

  public static UInt16 valueOf(final short value) {
    return new UInt16(value);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final UInt16 that = (UInt16) o;
    return delegate == that.delegate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return Integer.toUnsignedString(delegate);
  }

  @Override
  public Type getType() {
    return Type.UINT16;
  }

  @Override
  public Short getDelegate() {
    return delegate;
  }

  @Override
  public int intValue() {
    return Short.toUnsignedInt(delegate);
  }

  @Override
  public long longValue() {
    return Short.toUnsignedLong(delegate);
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
  public int compareTo(final UInt16 o) {
    return Short.toUnsignedInt(delegate) - Short.toUnsignedInt(o.delegate);
  }
}
