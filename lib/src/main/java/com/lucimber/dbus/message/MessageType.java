/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

/**
 * D-Bus message types as defined in the D-Bus wire protocol.
 * <p>
 * Each message begins with a single byte indicating its type.
 * </p>
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#message-protocol-types">D-Bus Specification: Message Protocol Types</a>
 * @since 1.0
 */
public enum MessageType {
  /**
   * Method call request. Bit mask: 0x01
   */
  METHOD_CALL((byte) 1),
  /**
   * Method return (reply). Bit mask: 0x02
   */
  METHOD_RETURN((byte) 2),
  /**
   * Error reply. Bit mask: 0x03
   */
  ERROR((byte) 3),
  /**
   * Signal emission. Bit mask: 0x04
   */
  SIGNAL((byte) 4);

  private final byte code;

  MessageType(byte code) {
    this.code = code;
  }

  /**
   * Returns the byte code corresponding to this message type.
   *
   * @return the D-Bus message type code
   */
  public byte getCode() {
    return code;
  }

  /**
   * Looks up a MessageType by its byte code.
   *
   * @param code the byte code from the wire
   * @return the matching MessageType
   * @throws IllegalArgumentException if the code is unrecognized
   */
  public static MessageType fromCode(byte code) {
    for (MessageType t : values()) {
      if (t.code == code) {
        return t;
      }
    }
    throw new IllegalArgumentException("Unknown D-Bus message type code: " + code);
  }
}
