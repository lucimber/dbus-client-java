package io.lucimber.dbus.type;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D-Bus objects are identified within an application via their object path.
 * The object path intentionally looks just like a standard Unix file system path.
 * The primary difference is that the path may contain only numbers, letters,
 * underscores, and the / character.
 * <p>
 * From a functional standpoint, the primary purpose of object paths is simply
 * to be a unique identifier for an object. The "hierarchy" implied the path structure
 * is almost purely conventional. Applications with a naturally hierarchical structure
 * will likely take advantage of this feature while others may choose to ignore it completely.
 *
 * @see <a href="https://pythonhosted.org/txdbus/dbus_overview.html">DBus Overview (Key Components)</a>
 */
public final class ObjectPath implements DBusBasicType {

    private static final Pattern PATTERN = Pattern.compile("^/|(/[a-zA-Z0-9_]+)+$");
    private final String delegate;

    private ObjectPath(final CharSequence sequence) {
        this.delegate = sequence.toString();
    }

    /**
     * Constructs a new {@link ObjectPath} instance by parsing a {@link CharSequence}.
     *
     * @param sequence The sequence composed of a valid object path.
     * @return A new instance of {@link ObjectPath}.
     * @throws ObjectPathException If the given {@link CharSequence} is not well formed.
     */
    public static ObjectPath valueOf(final CharSequence sequence) throws ObjectPathException {
        Objects.requireNonNull(sequence, "sequence must not be null");
        final Matcher matcher = PATTERN.matcher(sequence);
        if (matcher.matches()) {
            return new ObjectPath(sequence);
        } else {
            throw new ObjectPathException("invalid object path");
        }
    }

    public CharSequence getWrappedValue() {
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
        final ObjectPath path = (ObjectPath) o;
        return delegate.equals(path.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return delegate;
    }

    /**
     * Tests if this path start with the specified prefix.
     *
     * @param prefix the prefix
     * @return {@code true} if the object path represented by the argument is a prefix; {@code false} otherwise.
     */
    public boolean startsWith(final ObjectPath prefix) {
        return delegate.startsWith(prefix.delegate);
    }

    /**
     * Tests if this path ends with the specified suffix.
     *
     * @param suffix the suffix
     * @return {@code true} if the object path represented by the argument is a suffix; {@code false} otherwise.
     */
    public boolean endsWith(final ObjectPath suffix) {
        return delegate.endsWith(suffix.delegate);
    }

    @Override
    public Type getType() {
        return Type.OBJECT_PATH;
    }

    @Override
    public String getDelegate() {
        return delegate;
    }
}
