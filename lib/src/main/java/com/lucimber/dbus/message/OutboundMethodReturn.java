/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.List;
import java.util.Optional;

/**
 * An outbound method return message.
 *
 * @since 1.0
 */
public final class OutboundMethodReturn extends AbstractReply implements OutboundReply {

  private final DBusString dst;

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial      the serial number
   * @param replySerial the reply serial number
   * @param dst         optional; the destination of this method return
   * @param signature   optional; the signature of the message body
   * @param payload     optional; the message body
   */
  public OutboundMethodReturn(
          DBusUInt32 serial,
          DBusUInt32 replySerial,
          DBusString dst,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, replySerial, signature, payload);
    this.dst = dst;
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(dst);
  }

  @Override
  public String toString() {
    var s = "OutboundMethodReturn{dst='%s', serial='%s', replySerial='%s', sig=%s}";
    var mappedDst = getDestination().map(DBusString::toString).orElse("");
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, mappedDst, getSerial(), getReplySerial(), sig);
  }

  public static class Builder {
    private DBusUInt32 serial;
    private DBusUInt32 replySerial;
    private DBusString destination;
    private DBusSignature signature;
    private List<? extends DBusType> payload;

    private Builder() {
    }

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

    public Builder withDestination(DBusString destination) {
      this.destination = destination;
      return this;
    }

    public Builder withBody(DBusSignature signature, List<? extends DBusType> payload) {
      this.signature = signature;
      this.payload = payload;
      return this;
    }

    public OutboundMethodReturn build() {
      validate();
      return new OutboundMethodReturn(
              serial,
              replySerial,
              destination,
              signature,
              payload
      );
    }

    private void validate() {
      if (serial == null) {
        throw new InvalidMessageException("Serial must not be null.");
      }
      if (replySerial == null) {
        throw new InvalidMessageException("Reply serial must not be null.");
      }
      if (destination != null && destination.getDelegate().isBlank()) {
        throw new InvalidMessageException("Destination must not be blank.");
      }
      if (signature == null && payload != null) {
        throw new InvalidMessageException("Payload is present, but signature is missing "
                + "– both must be set together or left null.");
      } else if (signature != null && payload == null) {
        throw new InvalidMessageException("Signature is present, but payload is missing "
                + "– both must be set together or left null.");
      }
    }
  }
}
