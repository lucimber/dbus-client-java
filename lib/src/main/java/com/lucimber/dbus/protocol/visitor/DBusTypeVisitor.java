/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.visitor;

import com.lucimber.dbus.protocol.types.*;

/**
 * Visitor interface for D-Bus runtime types.
 *
 * @param <R> the return type of the visit methods
 * @since 2.0
 */
public interface DBusTypeVisitor<R> {

  R visitByte(BetterDBusByte value);

  R visitBoolean(BetterDBusBoolean value);

  R visitInt16(BetterDBusInt16 value);

  R visitUInt16(BetterDBusUInt16 value);

  R visitInt32(BetterDBusInt32 value);

  R visitUInt32(BetterDBusUInt32 value);

  R visitInt64(BetterDBusInt64 value);

  R visitUInt64(BetterDBusUInt64 value);

  R visitDouble(BetterDBusDouble value);

  R visitString(BetterDBusString value);

  R visitObjectPath(BetterDBusObjectPath value);

  R visitSignature(BetterDBusSignature value);

  R visitUnixFd(BetterDBusUnixFd value);

  R visitArray(BetterDBusArray value);

  R visitStruct(BetterDBusStruct value);

  R visitDictEntry(BetterDBusDictEntry value);

  R visitVariant(BetterDBusVariant value);
}