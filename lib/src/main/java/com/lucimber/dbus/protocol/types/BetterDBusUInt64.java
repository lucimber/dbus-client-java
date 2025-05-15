/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

import java.math.BigInteger;

/**
 * A D-Bus UINT64 (unsigned 64-bit) type.
 *
 * @param value the underlying unsigned value
 * @since 1.0
 */
public record BetterDBusUInt64(BigInteger value) implements BetterDBusType {

  public BetterDBusUInt64 {
    if (value == null || value.signum() < 0) {
      throw new IllegalArgumentException("Invalid UINT64 value: " + value);
    }
  }

  @Override
  public String signature() {
    return "t";
  }

  @Override
  public BigInteger getValue() {
    return value;
  }
}
