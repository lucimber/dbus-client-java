/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstract implementation of message.
 *
 * @since 1.0
 */
abstract class AbstractMessage implements Message {

    private final DBusUInt32 serial;
    private final List<DBusType> payload;
    private final DBusSignature signature;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param serial the serial number
     */
    AbstractMessage(DBusUInt32 serial) {
        this(serial, null, null);
    }

    /**
     * Constructs a new instance with all parameter.
     *
     * @param serial the serial number
     * @param signature optional; the signature of the message body
     * @param payload optional; the message body
     */
    AbstractMessage(DBusUInt32 serial, DBusSignature signature, List<? extends DBusType> payload) {
        this.serial = Objects.requireNonNull(serial);
        this.signature = signature;
        this.payload = (payload == null) ? Collections.emptyList() : new ArrayList<>(payload);
    }

    @Override
    public DBusUInt32 getSerial() {
        return serial;
    }

    @Override
    public List<DBusType> getPayload() {
        return new ArrayList<>(payload);
    }

    @Override
    public Optional<DBusSignature> getSignature() {
        return Optional.ofNullable(signature);
    }

    @Override
    public String toString() {
        var s = "AbstractMessage{serial='%s', sig='%s'}";
        var sig = getSignature().map(DBusSignature::toString).orElse("");
        return String.format(s, serial, sig);
    }
}
