/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * D-Bus objects are identified within an application via their object path. The object path
 * intentionally looks just like a standard Unix file system path. The primary difference is that
 * the path may contain only numbers, letters, underscores, and the / character.
 *
 * <p>From a functional standpoint, the primary purpose of object paths is simply to be a unique
 * identifier for an object. The "hierarchy" implied the path structure is almost purely
 * conventional. Applications with a naturally hierarchical structure will likely take advantage of
 * this feature while others may choose to ignore it completely.
 *
 * @see <a href="https://pythonhosted.org/txdbus/dbus_overview.html">DBus Overview (Key
 *     Components)</a>
 */
public final class DBusObjectPath implements DBusBasicType {

    private static final Pattern PATTERN = Pattern.compile("^/|(/[a-zA-Z0-9_]+)+$");
    private static final int MAX_PATH_LENGTH = 268435455; // 2^28 - 1 (256MB - 1)
    private final String delegate;

    private DBusObjectPath(final CharSequence sequence) {
        this.delegate = sequence.toString();
    }

    /**
     * Constructs a new {@link DBusObjectPath} instance by parsing a {@link CharSequence}. Validates
     * that the path is valid UTF-8, follows D-Bus object path syntax, and is within size limits.
     *
     * @param sequence The sequence composed of a valid object path.
     * @return A new instance of {@link DBusObjectPath}.
     * @throws ObjectPathException If the given {@link CharSequence} is not well formed.
     */
    public static DBusObjectPath valueOf(final CharSequence sequence) throws ObjectPathException {
        Objects.requireNonNull(sequence, "sequence must not be null");

        String pathStr = sequence.toString();

        // Validate UTF-8 and check for NUL characters
        byte[] utf8Bytes = pathStr.getBytes(StandardCharsets.UTF_8);
        String roundTrip = new String(utf8Bytes, StandardCharsets.UTF_8);
        if (!pathStr.equals(roundTrip)) {
            throw new ObjectPathException("Object path contains invalid UTF-8 sequences");
        }

        if (pathStr.contains("\u0000")) {
            throw new ObjectPathException("Object path must not contain NUL characters");
        }

        // Check size limit
        if (utf8Bytes.length > MAX_PATH_LENGTH) {
            throw new ObjectPathException(
                    "Object path too long: "
                            + utf8Bytes.length
                            + " bytes, maximum "
                            + MAX_PATH_LENGTH);
        }

        // Validate enhanced object path syntax
        if (!isValidObjectPath(pathStr)) {
            throw new ObjectPathException("Invalid object path syntax: " + pathStr);
        }

        return new DBusObjectPath(sequence);
    }

    /**
     * Enhanced validation for D-Bus object path syntax.
     *
     * @param path the path to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidObjectPath(String path) {
        // Basic pattern check
        final Matcher matcher = PATTERN.matcher(path);
        if (!matcher.matches()) {
            return false;
        }

        // Root path is always valid
        if (path.equals("/")) {
            return true;
        }

        // Additional edge case validation for non-root paths
        // No trailing slash (except root)
        if (path.endsWith("/")) {
            return false;
        }

        // No consecutive slashes
        if (path.contains("//")) {
            return false;
        }

        // Validate components (skip first empty element from split)
        String[] components = path.split("/");
        for (int i = 1; i < components.length; i++) { // Start at 1 to skip empty first element
            String component = components[i];
            if (component.isEmpty()) {
                return false; // Empty component (from consecutive slashes)
            }
        }

        return true;
    }

    /**
     * Gets the wrapped string value of this object path.
     *
     * @return a string value
     */
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
        final DBusObjectPath path = (DBusObjectPath) o;
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
     * @return {@code true} if the object path represented by the argument is a prefix; {@code
     *     false} otherwise.
     */
    public boolean startsWith(final DBusObjectPath prefix) {
        return delegate.startsWith(prefix.delegate);
    }

    /**
     * Tests if this path ends with the specified suffix.
     *
     * @param suffix the suffix
     * @return {@code true} if the object path represented by the argument is a suffix; {@code
     *     false} otherwise.
     */
    public boolean endsWith(final DBusObjectPath suffix) {
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
