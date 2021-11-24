package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps an unsigned {@link Long} to its D-Bus equivalent of UINT64.
 */
public final class UInt64 extends Number implements Comparable<UInt64>, DBusBasicType {

  private final long delegate;

  private UInt64(final long delegate) {
    this.delegate = delegate;
  }

  public static UInt64 valueOf(final long value) {
    return new UInt64(value);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final UInt64 that = (UInt64) o;
    return delegate == that.delegate;
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    return Long.toUnsignedString(delegate);
  }

  @Override
  public Type getType() {
    return Type.UINT64;
  }

  @Override
  public Long getDelegate() {
    return delegate;
  }

  @Override
  public int intValue() {
    return (int) delegate;
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
  public int compareTo(final UInt64 o) {
    return Long.compareUnsigned(delegate, o.delegate);
  }
}
