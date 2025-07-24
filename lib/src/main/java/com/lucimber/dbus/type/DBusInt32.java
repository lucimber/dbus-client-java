/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import java.util.Objects;

/** Maps an {@link Integer} to its D-Bus equivalent of INT32. */
public final class DBusInt32 extends Number implements Comparable<DBusInt32>, DBusBasicType {

    private final int delegate;

    private DBusInt32(final int delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a new D-Bus INT32 from its Java counterpart.
     *
     * @param value the integer value
     * @return a new instance
     */
    public static DBusInt32 valueOf(final int value) {
        return new DBusInt32(value);
    }

    @Override
    public Type getType() {
        return Type.INT32;
    }

    @Override
    public Integer getDelegate() {
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
        final DBusInt32 that = (DBusInt32) o;
        return delegate == that.delegate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return Integer.toString(delegate);
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
    public int compareTo(final DBusInt32 o) {
        return Integer.compare(delegate, o.delegate);
    }
}
