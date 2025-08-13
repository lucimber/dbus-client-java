/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.type;

import java.util.Objects;

/** Maps a {@link Short} to its D-Bus equivalent of INT16. */
public final class DBusInt16 extends Number implements Comparable<DBusInt16>, DBusBasicType {

    private final short delegate;

    private DBusInt16(final short delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a new D-Bus INT16 from its Java counterpart.
     *
     * @param value the short value
     * @return a new instance
     */
    public static DBusInt16 valueOf(final short value) {
        return new DBusInt16(value);
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
        final DBusInt16 int16 = (DBusInt16) o;
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
    public int compareTo(final DBusInt16 o) {
        return Short.compare(delegate, o.delegate);
    }
}
