/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.exception;

import com.lucimber.dbus.type.DBusString;

/** Authentication did not complete successfully. */
public final class AuthFailedException extends AbstractException {

    private static final DBusString ERROR_NAME =
            DBusString.valueOf("org.freedesktop.DBus.Error.AuthFailed");

    /** Creates a new instance. */
    public AuthFailedException() {
        super(ERROR_NAME);
    }

    /**
     * Creates a new instance with a message.
     *
     * @param message the message
     */
    public AuthFailedException(final DBusString message) {
        super(ERROR_NAME, message);
    }

    /**
     * Creates a new instance with a cause.
     *
     * @param cause the cause
     */
    public AuthFailedException(final Throwable cause) {
        super(ERROR_NAME, cause);
    }

    /**
     * Creates a new instance with a message and cause.
     *
     * @param message the message
     * @param cause the cause
     */
    public AuthFailedException(final DBusString message, final Throwable cause) {
        super(ERROR_NAME, message, cause);
    }
}
