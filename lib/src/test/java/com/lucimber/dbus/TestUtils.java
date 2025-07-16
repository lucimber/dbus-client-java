/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus;

import org.junit.jupiter.params.provider.Arguments;

import java.nio.ByteOrder;
import java.util.stream.Stream;

public class TestUtils {
  private TestUtils() {
    // Prevent instantiation
  }

  static Stream<Arguments> byteOrderProvider() {
    return Stream.of(
          Arguments.of(ByteOrder.BIG_ENDIAN),
          Arguments.of(ByteOrder.LITTLE_ENDIAN)
    );
  }
}
