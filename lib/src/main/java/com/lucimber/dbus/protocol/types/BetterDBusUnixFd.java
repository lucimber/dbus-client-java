/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * A D-Bus UNIX_FD type (file descriptor).
 *
 * @param fd the underlying file descriptor number
 * @since 1.0
 */
public record BetterDBusUnixFd(int fd) implements BetterDBusType {

  public BetterDBusUnixFd {
    if (fd < 0) {
      throw new IllegalArgumentException("Invalid UNIX_FD: " + fd);
    }
  }

  @Override
  public String signature() {
    return "h";
  }

  @Override
  public Integer getValue() {
    return fd;
  }
}
