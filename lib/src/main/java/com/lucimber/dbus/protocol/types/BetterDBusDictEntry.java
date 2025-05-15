/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

import java.util.Map;

/**
 * A D-Bus DICT_ENTRY type, wrapping a key/value pair.
 *
 * @param key   the DbusType key
 * @param value the DbusType value
 * @since 1.0
 */
public record BetterDBusDictEntry(BetterDBusType key, BetterDBusType value) implements BetterDBusType {

  public BetterDBusDictEntry {
    if (key == null || value == null) {
      throw new IllegalArgumentException("DictEntry key and value must not be null");
    }
  }

  @Override
  public String signature() {
    return "{" + key.signature() + value.signature() + "}";
  }

  @Override
  public java.util.Map.Entry<BetterDBusType, BetterDBusType> getValue() {
    return Map.entry(key, value);
  }
}
