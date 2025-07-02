/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

/**
 * D-Bus header field codes, as defined by the D-Bus specification.
 * <p>
 * Each header field is represented as a single byte code and
 * maps to a standard D-Bus header entry in the a{yv} dict.</p>
 *
 * @since 1.0
 */
public enum HeaderField {
  /**
   * Object path to the object receiving or emitting the message.
   */
  PATH((byte) 1),
  /**
   * Interface name associated with the message.
   */
  INTERFACE((byte) 2),
  /**
   * Member (method or signal) name.
   */
  MEMBER((byte) 3),
  /**
   * Error name for error messages.
   */
  ERROR_NAME((byte) 4),
  /**
   * Reply serial for method return messages.
   */
  REPLY_SERIAL((byte) 5),
  /**
   * Destination bus name.
   */
  DESTINATION((byte) 6),
  /**
   * Sender bus name.
   */
  SENDER((byte) 7),
  /**
   * Signature of the body parameters.
   */
  SIGNATURE((byte) 8),
  /**
   * Number of UNIX file descriptors in the message.
   */
  UNIX_FDS((byte) 9);

  private final byte code;

  HeaderField(final byte code) {
    this.code = code;
  }

  /**
   * Looks up a HeaderField by its byte code.
   *
   * @param code the byte code
   * @return the matching HeaderField
   * @throws IllegalArgumentException if no matching field exists
   */
  public static HeaderField fromCode(final byte code) {
    for (HeaderField field : values()) {
      if (field.code == code) {
        return field;
      }
    }
    throw new IllegalArgumentException("Unknown D-Bus header field code: " + code);
  }

  /**
   * Returns the byte code for this header field.
   *
   * @return the D-Bus header field code
   */
  public byte getCode() {
    return code;
  }
}
