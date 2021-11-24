package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps an unsigned {@link Integer} to its D-Bus equivalent of UINT32.
 */
public final class UInt32 extends Number implements Comparable<UInt32>, DBusBasicType {

  private final int delegate;

  private UInt32(final int delegate) {
    this.delegate = delegate;
  }

  public static UInt32 valueOf(final int value) {
    return new UInt32(value);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final UInt32 that = (UInt32) o;
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
    return Type.UINT32;
  }

  @Override
  public Integer getDelegate() {
    return delegate;
  }

  @Override
  public int intValue() {
    return delegate;
  }

  @Override
  public long longValue() {
    return Integer.toUnsignedLong(delegate);
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
  public int compareTo(final UInt32 o) {
    return Integer.compareUnsigned(delegate, o.delegate);
  }
}
