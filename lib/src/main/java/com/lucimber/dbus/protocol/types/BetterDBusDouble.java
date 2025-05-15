/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus DOUBLE (64-bit floating point) type.
 *
 * @param value the underlying double value
 * @since 1.0
 */
public record BetterDBusDouble(double value) implements BetterDBusType {

  @Override
  public String signature() {
    return "d";
  }

  @Override
  public Double getValue() {
    return value;
  }
}