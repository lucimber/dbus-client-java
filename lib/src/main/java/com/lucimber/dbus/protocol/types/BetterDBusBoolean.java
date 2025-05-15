/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus BOOLEAN type.
 *
 * @param value the underlying boolean value
 * @since 1.0
 */
public record BetterDBusBoolean(boolean value) implements BetterDBusType {

  @Override
  public String signature() {
    return "b";
  }

  @Override
  public Boolean getValue() {
    return value;
  }
}