/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.type;

/**
 * A {@link RuntimeException} that gets thrown by {@link DBusSignature}, if a marshalled {@link
 * DBusSignature} cannot be parsed.
 */
public class SignatureException extends RuntimeException {
    public SignatureException(final String message) {
        super(message);
    }

    public SignatureException(final Throwable cause) {
        super(cause);
    }

    public SignatureException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
