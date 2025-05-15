/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature.ast;

/**
 * Descriptor for a D-Bus array type, denoted by the prefix 'a'.
 *
 * <p>Holds the descriptor of its element type.</p>
 *
 * @param elementType the descriptor of the array element type
 * @since 2.0
 */
public record Array(TypeDescriptor elementType) implements TypeDescriptor {
  public Array {
    if (elementType == null) {
      throw new IllegalArgumentException("Element type must not be null");
    }
  }
}