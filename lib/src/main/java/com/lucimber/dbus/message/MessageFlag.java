/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

/**
 * D-Bus message flags as defined by the D-Bus wire protocol.
 * <p>
 * Flags correspond to individual bits in the message header's "flags" byte.</p>
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#message-protocol-flags">D-Bus Specification: Message Flags</a>
 * @since 1.0
 */
public enum MessageFlag {
  /**
   * No reply is expected for this method call message.
   * <p>Bit mask: 0x01</p>
   */
  NO_REPLY_EXPECTED((byte) 0x01),
  /**
   * Do not automatically start the destination service if not already running.
   * <p>Bit mask: 0x02</p>
   */
  NO_AUTO_START((byte) 0x02),
  /**
   * Allow the client to interactively prompt the user for authentication credentials.
   * <p>Bit mask: 0x04</p>
   */
  ALLOW_INTERACTIVE_AUTHORIZATION((byte) 0x04);

  private final byte code;

  MessageFlag(final byte code) {
    this.code = code;
  }

  /**
   * Returns the bit mask value for this flag.
   *
   * @return the flag's bit mask (0x01, 0x02, etc.)
   */
  public byte code() {
    return code;
  }
}
