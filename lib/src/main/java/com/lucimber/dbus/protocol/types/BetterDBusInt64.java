/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus INT64 (signed 64-bit) type.
 *
 * @param value the underlying long value
 * @since 1.0
 */
public record BetterDBusInt64(long value) implements BetterDBusType {

  @Override
  public String signature() {
    return "x";
  }

  @Override
  public Long getValue() {
    return value;
  }
}