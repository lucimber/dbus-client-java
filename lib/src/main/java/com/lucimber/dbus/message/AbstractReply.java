/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.List;
import java.util.Objects;

/**
 * An abstract implementation of a reply message.
 *
 * @since 1.0
 */
abstract class AbstractReply extends AbstractMessage {

    private final DBusUInt32 replySerial;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param serial the serial number
     * @param replySerial the reply serial number
     */
    AbstractReply(DBusUInt32 serial, DBusUInt32 replySerial) {
        super(serial);
        this.replySerial = Objects.requireNonNull(replySerial);
    }

    /**
     * Constructs a new instance with all parameter.
     *
     * @param serial the serial number
     * @param replySerial the reply serial number
     * @param signature optional; the signature of the message body
     * @param payload optional; the message body
     */
    AbstractReply(
            DBusUInt32 serial,
            DBusUInt32 replySerial,
            DBusSignature signature,
            List<? extends DBusType> payload) {
        super(serial, signature, payload);
        this.replySerial = Objects.requireNonNull(replySerial);
    }

    /**
     * Gets the serial number of the message this message is a reply to.
     *
     * @return The serial number as an {@link DBusUInt32}.
     */
    public DBusUInt32 getReplySerial() {
        return replySerial;
    }

    @Override
    public String toString() {
        var s = "AbstractReply{serial='%s', replySerial='%s', sig='%s'}";
        var sig = getSignature().map(DBusSignature::toString).orElse("");
        return String.format(s, getSerial(), replySerial, sig);
    }
}
