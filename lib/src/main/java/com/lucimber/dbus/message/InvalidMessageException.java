/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.message;

/** Signals that a message is invalid or violates the D-Bus specification. */
public final class InvalidMessageException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidMessageException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public InvalidMessageException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified cause.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public InvalidMessageException(final Throwable cause) {
        super(cause);
    }
}
