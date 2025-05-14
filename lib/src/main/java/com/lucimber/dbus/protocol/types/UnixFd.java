/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.protocol.types;

import java.util.Objects;

/**
 * Maps an unsigned {@link Integer} to its D-Bus equivalent of UNIX_FD.
 */
public final class UnixFd implements DBusBasicType {

  private final int delegate;

  private UnixFd(final int delegate) {
    this.delegate = delegate;
  }

  /**
   * Constructs a new D-Bus unix file descriptor.
   *
   * @param value the integer value
   * @return a new instance
   */
  public static UnixFd valueOf(final int value) {
    return new UnixFd(value);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final UnixFd that = (UnixFd) o;
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
    return Type.UNIX_FD;
  }

  @Override
  public Integer getDelegate() {
    return delegate;
  }
}
