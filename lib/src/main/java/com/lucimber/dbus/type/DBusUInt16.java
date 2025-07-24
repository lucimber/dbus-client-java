/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import java.util.Objects;

/** Maps an unsigned {@link Short} to its D-Bus equivalent of UINT16. */
public final class DBusUInt16 extends Number implements Comparable<DBusUInt16>, DBusBasicType {

    private final short delegate;

    private DBusUInt16(final short delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a new D-Bus UINT16 from its Java counterpart.
     *
     * @param value the short value
     * @return a new instance
     */
    public static DBusUInt16 valueOf(final short value) {
        return new DBusUInt16(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DBusUInt16 that = (DBusUInt16) o;
        return delegate == that.delegate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return Integer.toString(Short.toUnsignedInt(delegate));
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
        return Short.toUnsignedInt(delegate);
    }

    @Override
    public double doubleValue() {
        return Short.toUnsignedInt(delegate);
    }

    @Override
    public int compareTo(final DBusUInt16 o) {
        return Short.toUnsignedInt(delegate) - Short.toUnsignedInt(o.delegate);
    }
}
