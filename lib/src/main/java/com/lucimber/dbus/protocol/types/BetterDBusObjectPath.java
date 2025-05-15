/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus object path, e.g. "/org/freedesktop/NetworkManager".
 *
 * @param path the raw path string
 */
public record BetterDBusObjectPath(String path) implements BetterDBusType {

  public BetterDBusObjectPath {
    if (!path.matches("(/[:A-Za-z0-9_]+)+")) {
      throw new IllegalArgumentException("Invalid D-Bus object path: " + path);
    }
  }

  @Override
  public String signature() {
    return "o";
  }

  @Override
  public String getValue() {
    return path;
  }
}
