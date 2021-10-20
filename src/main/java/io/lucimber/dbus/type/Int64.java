package io.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps a {@link Long} to its D-Bus equivalent of INT64.
 */
public final class Int64 extends Number implements Comparable<Int64>, DBusBasicType {

    private final long delegate;

    private Int64(final long delegate) {
        this.delegate = delegate;
    }

    public static Int64 valueOf(final long value) {
        return new Int64(value);
    }

    @Override
    public Type getType() {
        return Type.INT64;
    }

    @Override
    public Long getDelegate() {
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
        final Int64 int64 = (Int64) o;
        return delegate == int64.delegate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return Long.toString(delegate);
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
    public int compareTo(final Int64 o) {
        return Long.compare(delegate, o.delegate);
    }
}
