/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus UINT16 (unsigned 16-bit) type.
 *
 * @param value the underlying integer value (0..65535)
 * @since 1.0
 */
public record BetterDBusUInt16(int value) implements BetterDBusType {

  public BetterDBusUInt16 {
    if (value < 0 || value > 0xFFFF) {
      throw new IllegalArgumentException("Invalid UINT16 value: " + value);
    }
  }

  @Override
  public String signature() {
    return "q";
  }

  @Override
  public Integer getValue() {
    return value;
  }
}
