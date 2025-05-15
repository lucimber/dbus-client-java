/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus UINT32 (unsigned 32-bit) type.
 *
 * @param value the underlying long value (0..2^32-1)
 * @since 1.0
 */
public record BetterDBusUInt32(long value) implements BetterDBusType {

  public BetterDBusUInt32 {
    if (value < 0 || value > 0xFFFFFFFFL) {
      throw new IllegalArgumentException("Invalid UINT32 value: " + value);
    }
  }

  @Override
  public String signature() {
    return "u";
  }

  @Override
  public Long getValue() {
    return value;
  }
}
