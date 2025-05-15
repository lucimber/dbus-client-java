/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus INT16 (signed 16-bit) type.
 *
 * @param value the underlying short value
 * @since 1.0
 */
public record BetterDBusInt16(short value) implements BetterDBusType {

  @Override
  public String signature() {
    return "n";
  }

  @Override
  public Short getValue() {
    return value;
  }
}