package io.lucimber.dbus.type;

import java.util.Objects;

/**
 * Maps an {@link Integer} to its D-Bus equivalent of INT32.
 */
public final class Int32 extends Number implements Comparable<Int32>, DBusBasicType {

    private final int delegate;

    private Int32(final int delegate) {
        this.delegate = delegate;
    }

    public static Int32 valueOf(final int value) {
        return new Int32(value);
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
        final Int32 that = (Int32) o;
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
    public int compareTo(final Int32 o) {
        return delegate - o.delegate;
    }
}
