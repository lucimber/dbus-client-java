/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus VARIANT type, wrapping an arbitrary D-Bus value.
 *
 * @param value the DbusType instance held by this variant
 * @since 1.0
 */
public record BetterDBusVariant(BetterDBusType value) implements BetterDBusType {

  public BetterDBusVariant {
    if (value == null) {
      throw new IllegalArgumentException("Variant value must not be null");
    }
  }

  @Override
  public String signature() {
    return "v";
  }

  @Override
  public BetterDBusType getValue() {
    return value;
  }
}