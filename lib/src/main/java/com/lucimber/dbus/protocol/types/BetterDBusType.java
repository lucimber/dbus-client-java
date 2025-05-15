/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * Sealed root for all D-Bus runtime types.
 */
public sealed interface BetterDBusType
      permits
      BetterDBusByte,
      BetterDBusBoolean,
      BetterDBusInt16,
      BetterDBusUInt16,
      BetterDBusInt32,
      BetterDBusUInt32,
      BetterDBusInt64,
      BetterDBusUInt64,
      BetterDBusDouble,
      BetterDBusString,
      BetterDBusObjectPath,
      BetterDBusSignature,
      BetterDBusUnixFd,
      BetterDBusArray,
      BetterDBusStruct,
      BetterDBusDictEntry,
      BetterDBusVariant {
  /**
   * Returns the D-Bus type signature for this instance.
   * <p>
   * For basic types, this is a single character (e.g. "i", "s").
   * For containers, a composite signature (e.g. "a{sv}", "(ii)").
   * </p>
   *
   * @return the D-Bus type signature
   */
  String signature();

  /**
   * Returns the underlying Java value of this D-Bus type.
   *
   * @return the Java representation (e.g. Integer, String, List, Map)
   */
  Object getValue();
}
