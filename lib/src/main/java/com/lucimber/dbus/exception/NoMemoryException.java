/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/** A memory allocation failed, and the requested operation could not be completed. */
public final class NoMemoryException extends AbstractException {

    private static final DBusString ERROR_NAME =
            DBusString.valueOf("org.freedesktop.DBus.Error.NoMemory");

    /** Creates a new instance. */
    public NoMemoryException() {
        super(ERROR_NAME);
    }

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public NoMemoryException(final DBusString message) {
        super(ERROR_NAME, message);
    }

    /**
     * Creates a new instance with a cause.
     *
     * @param cause the cause
     */
    public NoMemoryException(final Throwable cause) {
        super(ERROR_NAME, cause);
    }

    /**
     * Creates a new instance with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public NoMemoryException(final DBusString message, final Throwable cause) {
        super(ERROR_NAME, message, cause);
    }
}
