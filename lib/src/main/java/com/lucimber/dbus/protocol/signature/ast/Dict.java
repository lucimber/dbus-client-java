/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature.ast;

/**
 * Descriptor for a D-Bus dictionary entry type, denoted by '{' key type value type '}'.
 *
 * <p>Holds descriptors for the key and value types.</p>
 *
 * @param keyType   the descriptor of the dictionary key type
 * @param valueType the descriptor of the dictionary value type
 * @since 2.0
 */
public record Dict(TypeDescriptor keyType, TypeDescriptor valueType) implements TypeDescriptor {
  public Dict {
    if (keyType == null || valueType == null) {
      throw new IllegalArgumentException("Key and value types must not be null");
    }
  }
}