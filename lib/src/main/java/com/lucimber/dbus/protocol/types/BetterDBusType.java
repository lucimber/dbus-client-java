/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.types;

import com.lucimber.dbus.protocol.visitor.DBusTypeVisitor;

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

  /**
   * Accepts a {@link DBusTypeVisitor} to perform operations based on
   * the concrete D-Bus type instance.
   *
   * @param visitor the visitor to dispatch to
   * @param <R> the return type of the visitor methods
   * @return the result of visiting this type
   */
  default <R> R accept(final DBusTypeVisitor<R> visitor) {
    if (this instanceof BetterDBusByte b)      return visitor.visitByte(b);
    if (this instanceof BetterDBusBoolean b)   return visitor.visitBoolean(b);
    if (this instanceof BetterDBusInt16 b)     return visitor.visitInt16(b);
    if (this instanceof BetterDBusUInt16 b)    return visitor.visitUInt16(b);
    if (this instanceof BetterDBusInt32 b)     return visitor.visitInt32(b);
    if (this instanceof BetterDBusUInt32 b)    return visitor.visitUInt32(b);
    if (this instanceof BetterDBusInt64 b)     return visitor.visitInt64(b);
    if (this instanceof BetterDBusUInt64 b)    return visitor.visitUInt64(b);
    if (this instanceof BetterDBusDouble b)    return visitor.visitDouble(b);
    if (this instanceof BetterDBusString b)    return visitor.visitString(b);
    if (this instanceof BetterDBusObjectPath b) return visitor.visitObjectPath(b);
    if (this instanceof BetterDBusSignature b) return visitor.visitSignature(b);
    if (this instanceof BetterDBusUnixFd b)    return visitor.visitUnixFd(b);
    if (this instanceof BetterDBusArray b)     return visitor.visitArray(b);
    if (this instanceof BetterDBusStruct b)    return visitor.visitStruct(b);
    if (this instanceof BetterDBusDictEntry b) return visitor.visitDictEntry(b);
    if (this instanceof BetterDBusVariant b)   return visitor.visitVariant(b);
    throw new IllegalStateException("Unknown DBusType: " + this.getClass());
  }
}
