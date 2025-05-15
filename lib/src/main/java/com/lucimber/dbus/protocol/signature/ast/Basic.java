/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature.ast;

/**
 * Descriptor for a basic (primitive) D-Bus type,
 * identified by its single‐character code (e.g. 'i', 's', 'b', …).
 *
 * @param code the D-Bus type code
 * @since 2.0
 */
public record Basic(char code) implements TypeDescriptor {
  public Basic {
    if (!"ybnqiuxtdsog".contains(String.valueOf(code))) {
      throw new IllegalArgumentException("Invalid basic type code: " + code);
    }
  }
}
