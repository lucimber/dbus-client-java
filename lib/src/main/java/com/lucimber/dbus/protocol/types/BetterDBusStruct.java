/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

import java.util.List;

/**
 * A D-Bus STRUCT type, wrapping an ordered list of members.
 *
 * @param members the non-null, non-empty list of DbusType members
 * @since 1.0
 */
public record BetterDBusStruct(List<BetterDBusType> members) implements BetterDBusType {

  public BetterDBusStruct {
    if (members == null || members.isEmpty()) {
      throw new IllegalArgumentException("Struct members must not be null or empty");
    }
  }

  @Override
  public String signature() {
    StringBuilder sb = new StringBuilder("(");
    for (BetterDBusType t : members) {
      sb.append(t.signature());
    }
    sb.append(')');
    return sb.toString();
  }

  @Override
  public List<BetterDBusType> getValue() {
    return members;
  }
}