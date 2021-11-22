package com.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps a {@link Byte} to its D-Bus equivalent of BYTE.
 */
public final class DBusByte extends Number implements Comparable<DBusByte>, DBusBasicType {

    private final byte delegate;

    private DBusByte(final byte delegate) {
        this.delegate = delegate;
    }

    public static DBusByte valueOf(final byte value) {
        return new DBusByte(value);
    }

    @Override
    public Type getType() {
        return Type.BYTE;
    }

    @Override
    public Byte getDelegate() {
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
        final DBusByte dBusByte = (DBusByte) o;
        return delegate == dBusByte.delegate;
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
    public int intValue() {
        return Byte.toUnsignedInt(delegate);
    }

    @Override
    public long longValue() {
        return Byte.toUnsignedLong(delegate);
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
    public int compareTo(final DBusByte o) {
        return Byte.toUnsignedInt(delegate) - Byte.toUnsignedInt(o.delegate);
    }
}
