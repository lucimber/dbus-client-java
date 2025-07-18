/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

/**
 * A {@link RuntimeException} that gets thrown by an {@link Encoder},
 * if the encoding of a value isn't possible.
 */
public class EncoderException extends RuntimeException {
  /**
   * Creates a new instance with a message.
   *
   * @param message the associated message.
   */
  public EncoderException(final String message) {
    super(message);
  }

  /**
   * Creates a new instance with a cause.
   *
   * @param cause the associated cause
   */
  public EncoderException(final Throwable cause) {
    super(cause);
  }

  /**
   * Creates a new instance with a message and a cause.
   *
   * @param message the associated message
   * @param cause   the associated cause
   */
  public EncoderException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
