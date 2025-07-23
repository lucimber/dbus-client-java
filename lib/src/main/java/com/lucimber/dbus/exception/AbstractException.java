/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;
import java.util.Objects;

/** Base class for exceptions related to this framework. */
public abstract class AbstractException extends Exception {
    private final DBusString errorName;

    /**
     * Constructs a new exception with the name of the error.
     *
     * @param errorName name of the error; e.g. {@code org.freedesktop.DBus.Error.AccessDenied}
     */
    public AbstractException(final DBusString errorName) {
        this.errorName = Objects.requireNonNull(errorName, "error name");
    }

    /**
     * Constructs a new exception with the name of the error and detail message.
     *
     * @param errorName name of the error; e.g. {@code org.freedesktop.DBus.Error.AccessDenied}
     * @param message detail message
     */
    public AbstractException(final DBusString errorName, final DBusString message) {
        super(message.toString());
        this.errorName = Objects.requireNonNull(errorName, "error name");
    }

    /**
     * Constructs a new exception with the name of the error and cause.
     *
     * @param errorName name of the error; e.g. {@code org.freedesktop.DBus.Error.AccessDenied}
     * @param cause the cause
     */
    public AbstractException(final DBusString errorName, final Throwable cause) {
        super(cause);
        this.errorName = Objects.requireNonNull(errorName, "error name");
    }

    /**
     * Constructs a new exception with the name of the error, detail message and cause.
     *
     * @param errorName name of the error; e.g. {@code org.freedesktop.DBus.Error.AccessDenied}
     * @param message the detail message
     * @param cause the cause
     */
    public AbstractException(
            final DBusString errorName, final DBusString message, final Throwable cause) {
        super(message.toString(), cause);
        this.errorName = Objects.requireNonNull(errorName, "error name");
    }

    /**
     * Gets the name of the error. E.g. {@code org.freedesktop.DBus.Error.AccessDenied}
     *
     * @return name of error as {@link DBusString}
     */
    public DBusString getErrorName() {
        return errorName;
    }
}
