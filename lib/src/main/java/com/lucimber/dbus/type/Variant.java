/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Variants may contain a value of any type.
 * The marshalled value of the variant includes
 * the D-Bus signature defining the type of data it contains.
 *
 * @see <a href="https://pythonhosted.org/txdbus/dbus_overview.html">DBus Overview (Key Components)</a>
 * @see DBusContainerType
 */
public final class Variant implements DBusContainerType {

  private final DBusType delegate;

  private Variant(final DBusType delegate) {
    this.delegate = Objects.requireNonNull(delegate);
  }

  public static Variant valueOf(final DBusType value) {
    return new Variant(value);
  }

  @Override
  public Type getType() {
    return Type.VARIANT;
  }

  @Override
  public DBusType getDelegate() {
    return delegate;
  }

  @Override
  public Signature getSignature() {
    final String s = String.valueOf(Type.VARIANT.getCode().getChar());
    return Signature.valueOf(s);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final Variant variant = (Variant) o;
    return Objects.equals(delegate, variant.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate);
  }

  @Override
  public String toString() {
    if (delegate instanceof DBusBasicType) {
      final DBusBasicType basicType = (DBusBasicType) delegate;
      return String.format("v[%c]", basicType.getType().getCode().getChar());
    } else if (delegate instanceof DBusContainerType) {
      final DBusContainerType containerType = (DBusContainerType) delegate;
      return String.format("v[%s]", containerType.getSignature());
    } else {
      return "v[unknown type]";
    }
  }
}
