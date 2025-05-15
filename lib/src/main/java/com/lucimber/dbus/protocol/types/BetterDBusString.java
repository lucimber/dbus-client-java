/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus STRING type.
 *
 * @param value the underlying non-null string
 * @since 1.0
 */
public record BetterDBusString(String value) implements BetterDBusType {

  public BetterDBusString {
    if (value == null) {
      throw new IllegalArgumentException("String value must not be null");
    }
  }

  @Override
  public String signature() {
    return "s";
  }

  @Override
  public String getValue() {
    return value;
  }
}
