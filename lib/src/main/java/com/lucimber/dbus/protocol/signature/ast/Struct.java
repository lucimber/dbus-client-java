/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature.ast;

import java.util.List;
import java.util.Objects;

/**
 * Descriptor for a D-Bus struct type, denoted by '(' member1 member2 ... ')'.
 *
 * <p>Holds an ordered list of member type descriptors.</p>
 *
 * @param members the ordered list of struct member descriptors
 * @since 2.0
 */
public record Struct(List<TypeDescriptor> members) implements TypeDescriptor {
  public Struct {
    if (members == null) {
      throw new IllegalArgumentException("Members list must not be null");
    }
    if (members.isEmpty()) {
      throw new IllegalArgumentException("Struct must contain at least one member");
    }
    if (members.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Struct member descriptors must not be null");
    }
  }
}