/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

import java.util.List;

/**
 * A D-Bus ARRAY type, wrapping a list of elements.
 *
 * @param elements the non-null list of DbusType elements
 * @since 1.0
 */
public record BetterDBusArray(List<BetterDBusType> elements) implements BetterDBusType {

  public BetterDBusArray {
    if (elements == null) {
      throw new IllegalArgumentException("Array elements must not be null");
    }
  }

  @Override
  public String signature() {
    if (elements.isEmpty()) {
      return "a"; // generic empty array
    }
    return "a" + elements.get(0).signature();
  }

  @Override
  public List<BetterDBusType> getValue() {
    return elements;
  }
}
