/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus SIGNATURE type, e.g. "aya{it}ib".
 *
 * @param signature the underlying non-null signature string
 * @since 1.0
 */
public record BetterDBusSignature(String signature) implements BetterDBusType {

  public BetterDBusSignature {
    if (signature == null
          || signature.length() > 255
          || !signature.matches("[aybnqiuxtdsog\\{\\}\\(\\)]+")
    ) {
      throw new IllegalArgumentException("Invalid D-Bus signature: " + signature);
    }
  }

  @Override
  public String signature() {
    return "g";
  }

  @Override
  public String getValue() {
    return signature;
  }
}
