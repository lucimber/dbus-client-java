/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import java.util.concurrent.atomic.AtomicLong;

import io.netty.util.AttributeKey;

import com.lucimber.dbus.type.DBusString;

final class DBusChannelAttribute {

    /**
     * Channel attribute key for the D-Bus session’s serial counter.
     *
     * <p>This attribute holds an {@link AtomicLong} instance used to generate unique, incrementing
     * serial numbers for outgoing D-Bus messages on a per-connection basis.
     */
    public static final AttributeKey<AtomicLong> SERIAL_COUNTER =
            AttributeKey.valueOf("DBUS_SERIAL_COUNTER");

    /**
     * Channel attribute key for storing the unique bus name assigned by the bus after a successful
     * Hello() call.
     */
    public static final AttributeKey<DBusString> ASSIGNED_BUS_NAME =
            AttributeKey.valueOf("DBUS_ASSIGNED_BUS_NAME");

    private DBusChannelAttribute() {} // Prevent instantiation
}
