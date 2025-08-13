/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/** The requested file could not be found. */
public final class FileNotFoundException extends AbstractException {

    private static final DBusString ERROR_NAME =
            DBusString.valueOf("org.freedesktop.DBus.Error.FileNotFound");

    /** Creates a new instance. */
    public FileNotFoundException() {
        super(ERROR_NAME);
    }

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public FileNotFoundException(final DBusString message) {
        super(ERROR_NAME, message);
    }

    /**
     * Creates a new instance with a cause.
     *
     * @param cause the cause
     */
    public FileNotFoundException(final Throwable cause) {
        super(ERROR_NAME, cause);
    }

    /**
     * Creates a new instance with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public FileNotFoundException(final DBusString message, final Throwable cause) {
        super(ERROR_NAME, message, cause);
    }
}
