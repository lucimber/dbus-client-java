/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature;

/**
 * Thrown when a D-Bus type signature cannot be parsed or is invalid.
 *
 * @since 2.0
 */
public class SignatureParseException extends RuntimeException {

  public SignatureParseException(final String message) {
    super(message);
  }

  public SignatureParseException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
