/*
 * SPDX-FileCopyrightText: 2023-2026 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

public class SaslMechanismException extends Exception {
    public SaslMechanismException(String message) {
        super(message);
    }

    public SaslMechanismException(String message, Throwable cause) {
        super(message, cause);
    }
}
