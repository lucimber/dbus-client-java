/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Variants may contain a value of any type. The marshalled value of the variant includes the D-Bus
 * signature defining the type of data it contains.
 *
 * @see <a href="https://pythonhosted.org/txdbus/dbus_overview.html">DBus Overview (Key
 *     Components)</a>
 * @see DBusContainerType
 */
public final class DBusVariant implements DBusContainerType {

    private final DBusType delegate;

    private DBusVariant(final DBusType delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    public static DBusVariant valueOf(final DBusType value) {
        return new DBusVariant(value);
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
    public DBusSignature getSignature() {
        final String s = String.valueOf(Type.VARIANT.getCode().getChar());
        return DBusSignature.valueOf(s);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DBusVariant variant = (DBusVariant) o;
        return Objects.equals(delegate, variant.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        if (delegate instanceof DBusBasicType basicType) {
            return String.format("v[%c]", basicType.getType().getCode().getChar());
        } else if (delegate instanceof DBusContainerType containerType) {
            return String.format("v[%s]", containerType.getSignature());
        } else {
            return "v[unknown type]";
        }
    }
}
