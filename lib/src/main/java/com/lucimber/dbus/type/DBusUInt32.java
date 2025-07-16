/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps an unsigned {@link Integer} to its D-Bus equivalent of UINT32.
 */
public final class DBusUInt32 extends Number implements Comparable<DBusUInt32>, DBusBasicType {

  private final int delegate;

  private DBusUInt32(final int delegate) {
    this.delegate = delegate;
  }

  /**
   * Constructs a new D-Bus UIN32 from its Java counterpart.
   *
   * @param value the integer value
   * @return a new instance
   */
  public static DBusUInt32 valueOf(final int value) {
    return new DBusUInt32(value);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final DBusUInt32 that = (DBusUInt32) o;
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
  public int compareTo(final DBusUInt32 o) {
    return Integer.compareUnsigned(delegate, o.delegate);
  }
}
