/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import java.util.List;
import java.util.Objects;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;

/**
 * An inbound error message.
 *
 * @since 1.0
 */
public final class InboundError extends AbstractReply implements InboundReply {

    private final DBusString sender;
    private final DBusString errorName;

    /**
     * Constructs a new instance with mandatory parameter.
     *
     * @param serial the serial number
     * @param replySerial the reply serial number
     * @param sender the origin of this error message
     * @param errorName the name of the error
     */
    public InboundError(
            DBusUInt32 serial, DBusUInt32 replySerial, DBusString sender, DBusString errorName) {
        super(serial, replySerial);
        this.sender = Objects.requireNonNull(sender);
        this.errorName = Objects.requireNonNull(errorName);
    }

    /**
     * Constructs a new instance with all parameter.
     *
     * @param serial the serial number
     * @param replySerial the reply serial number
     * @param sender the origin of this error message
     * @param errorName the name of the error
     * @param signature optional; the signature of the message body
     * @param payload optional; the message body
     */
    public InboundError(
            DBusUInt32 serial,
            DBusUInt32 replySerial,
            DBusString sender,
            DBusString errorName,
            DBusSignature signature,
            List<? extends DBusType> payload) {
        super(serial, replySerial, signature, payload);
        this.sender = Objects.requireNonNull(sender);
        this.errorName = Objects.requireNonNull(errorName);
    }

    @Override
    public DBusString getSender() {
        return sender;
    }

    /**
     * Returns the name of this error.
     *
     * @return a {@link DBusString}
     */
    public DBusString getErrorName() {
        return errorName;
    }

    @Override
    public String toString() {
        var s = "InboundError{sender='%s', serial='%s', replySerial='%s', name='%s', sig='%s'}";
        var sig = getSignature().map(DBusSignature::toString).orElse("");
        return String.format(s, sender, getSerial(), getReplySerial(), errorName, sig);
    }

    public static class Builder {
        private DBusUInt32 serial;
        private DBusUInt32 replySerial;
        private DBusString sender;
        private DBusString errorName;
        private DBusSignature signature;
        private List<? extends DBusType> payload;

        private Builder() {}

        public static Builder create() {
            return new Builder();
        }

        public Builder withSerial(DBusUInt32 serial) {
            this.serial = serial;
            return this;
        }

        public Builder withReplySerial(DBusUInt32 replySerial) {
            this.replySerial = replySerial;
            return this;
        }

        public Builder withSender(DBusString sender) {
            this.sender = sender;
            return this;
        }

        public Builder withErrorName(DBusString errorName) {
            this.errorName = errorName;
            return this;
        }

        public Builder withBody(DBusSignature signature, List<? extends DBusType> payload) {
            this.signature = signature;
            this.payload = payload;
            return this;
        }

        public InboundError build() {
            validate();
            return new InboundError(serial, replySerial, sender, errorName, signature, payload);
        }

        private void validate() {
            if (serial == null) {
                throw new InvalidMessageException("Serial must not be null.");
            }
            if (replySerial == null) {
                throw new InvalidMessageException("Reply serial must not be null.");
            }
            if (sender == null) {
                throw new InvalidMessageException("Sender must not be null.");
            } else if (sender.getDelegate().isBlank()) {
                throw new InvalidMessageException("Sender must not be blank.");
            }
            if (errorName == null) {
                throw new InvalidMessageException("Error name must not be null.");
            } else if (errorName.getDelegate().isBlank()) {
                throw new InvalidMessageException("Error name must not be blank.");
            }
            if (signature == null && payload != null) {
                throw new InvalidMessageException(
                        "Payload is present, but signature is missing "
                                + "– both must be set together or left null.");
            } else if (signature != null && payload == null) {
                throw new InvalidMessageException(
                        "Signature is present, but payload is missing "
                                + "– both must be set together or left null.");
            }
        }
    }
}
