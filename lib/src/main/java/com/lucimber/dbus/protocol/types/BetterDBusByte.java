/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus BYTE (unsigned 8-bit) type.
 *
 * @param value the underlying byte value
 * @since 1.0
 */
public record BetterDBusByte(byte value) implements BetterDBusType {

  @Override
  public String signature() {
    return "y";
  }

  @Override
  public Byte getValue() {
    return value;
  }
}