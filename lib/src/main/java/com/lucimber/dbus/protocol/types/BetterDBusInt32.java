/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus INT32 (signed 32-bit) type.
 *
 * @param value the underlying int value
 * @since 1.0
 */
public record BetterDBusInt32(int value) implements BetterDBusType {

  @Override
  public String signature() {
    return "i";
  }

  @Override
  public Integer getValue() {
    return value;
  }
}