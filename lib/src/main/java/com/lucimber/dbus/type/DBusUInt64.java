/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

import java.util.Objects;

/** Maps an unsigned {@link Long} to its D-Bus equivalent of UINT64. */
public final class DBusUInt64 extends Number implements Comparable<DBusUInt64>, DBusBasicType {

    private final long delegate;

    private DBusUInt64(final long delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a new D-Bus UINT64 from its Java counterpart.
     *
     * @param value the long value
     * @return a new instance
     */
    public static DBusUInt64 valueOf(final long value) {
        return new DBusUInt64(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DBusUInt64 that = (DBusUInt64) o;
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
        // Handle unsigned long to float conversion
        if (delegate >= 0) {
            return (float) delegate;
        } else {
            // For negative values in the signed representation,
            // we need to add 2^64 to get the unsigned value
            return (float) (delegate + Math.pow(2, 64));
        }
    }

    @Override
    public double doubleValue() {
        // Handle unsigned long to double conversion
        if (delegate >= 0) {
            return (double) delegate;
        } else {
            // For negative values in the signed representation,
            // we need to add 2^64 to get the unsigned value
            return (double) (delegate + Math.pow(2, 64));
        }
    }

    @Override
    public int compareTo(final DBusUInt64 o) {
        return Long.compareUnsigned(delegate, o.delegate);
    }
}
