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

/** Maps a {@link String} to its D-Bus equivalent of STRING. */
public final class DBusString implements DBusBasicType {

    private static final int MAX_STRING_LENGTH = 268435455; // 2^28 - 1 (256MB - 1)
    private final String delegate;

    private DBusString(final String delegate) {
        this.delegate = delegate;
    }

    /**
     * Constructs a new D-Bus string from its Java counterpart. Validates that the string is valid
     * UTF-8 and within size limits.
     *
     * @param value the string value
     * @return a new instance
     * @throws IllegalArgumentException if the string is invalid
     */
    public static DBusString valueOf(final String value) {
        Objects.requireNonNull(value, "value must not be null");

        // Validate UTF-8 by encoding and checking for replacement characters
        byte[] utf8Bytes = value.getBytes(StandardCharsets.UTF_8);
        String roundTrip = new String(utf8Bytes, StandardCharsets.UTF_8);
        if (!value.equals(roundTrip)) {
            throw new IllegalArgumentException("String contains invalid UTF-8 sequences");
        }

        // Check for NUL characters (not allowed in D-Bus strings)
        if (value.contains("\u0000")) {
            throw new IllegalArgumentException("String must not contain NUL characters");
        }

        // Check size limit (UTF-8 byte length)
        if (utf8Bytes.length > MAX_STRING_LENGTH) {
            throw new IllegalArgumentException(
                    "String too long: "
                            + utf8Bytes.length
                            + " bytes, maximum "
                            + MAX_STRING_LENGTH);
        }

        return new DBusString(value);
    }

    @Override
    public Type getType() {
        return Type.STRING;
    }

    @Override
    public String getDelegate() {
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
        final DBusString that = (DBusString) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return delegate;
    }
}
