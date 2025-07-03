/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import java.util.List;
import java.util.Optional;

/**
 * An outbound method call.
 *
 * @since 1.0
 */
public final class OutboundMethodCall extends AbstractMethodCall implements OutboundMessage {

  private final DBusString dst;
  private final boolean replyExpected;

  /**
   * Constructs a new instance with all parameter.
   * Please use the builder instead.
   *
   * @param serial        the serial number
   * @param path          the object path
   * @param member        the name of the method
   * @param replyExpected states if reply is expected
   * @param dst           optional; the destination of this method call
   * @param iface         optional; the name of the interface
   * @param signature     optional; the signature of the message body
   * @param payload       optional; the message body
   */
  public OutboundMethodCall(
          UInt32 serial,
          ObjectPath path,
          DBusString member,
          boolean replyExpected,
          DBusString dst,
          DBusString iface,
          Signature signature,
          List<? extends DBusType> payload) {
    super(serial, path, member, iface, signature, payload);
    this.dst = dst;
    this.replyExpected = replyExpected;
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(dst);
  }

  /**
   * States if the sender expects a reply to this method call or not.
   *
   * @return {@code TRUE} if reply is expected, {@code FALSE} otherwise.
   */
  public boolean isReplyExpected() {
    return replyExpected;
  }

  @Override
  public String toString() {
    var s = "OutboundMethodCall{dst='%s', serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var dst = getDestination().map(DBusString::toString).orElse("");
    var iface = getInterfaceName().map(DBusString::toString).orElse("");
    var sig = getSignature().map(Signature::toString).orElse("");
    return String.format(s, dst, getSerial(), getObjectPath(), iface, getMember(), sig);
  }

  public static class Builder {

    private UInt32 serial;
    private ObjectPath path;
    private DBusString destination;
    private DBusString iface;
    private DBusString member;
    private boolean replyExpected;
    private Signature signature;
    private List<? extends DBusType> payload;

    private Builder() {
      replyExpected = false;
    }

    public static Builder create() {
      return new Builder();
    }

    public Builder withSerial(UInt32 serial) {
      this.serial = serial;
      return this;
    }

    public Builder withPath(ObjectPath path) {
      this.path = path;
      return this;
    }

    public Builder withMember(DBusString member) {
      this.member = member;
      return this;
    }

    public Builder withReplyExpected(boolean replyExpected) {
      this.replyExpected = replyExpected;
      return this;
    }

    public Builder withDestination(DBusString destination) {
      this.destination = destination;
      return this;
    }

    public Builder withInterface(DBusString iface) {
      this.iface = iface;
      return this;
    }

    public Builder withBody(Signature signature, List<? extends DBusType> payload) {
      this.signature = signature;
      this.payload = payload;
      return this;
    }

    public OutboundMethodCall build() {
      validate();
      return new OutboundMethodCall(
              serial,
              path,
              member,
              replyExpected,
              destination,
              iface,
              signature,
              payload
      );
    }

    private void validate() {
      if (serial == null) {
        throw new InvalidMessageException("Serial number must not be null.");
      }
      if (path == null) {
        throw new InvalidMessageException("Object path must not be null.");
      }
      if (member == null) {
        throw new InvalidMessageException("Member name must not be null.");
      } else if (member.getDelegate().isBlank()) {
        throw new InvalidMessageException("Member name must be set and not blank.");
      }
      if (destination != null && destination.getDelegate().isBlank()) {
        throw new InvalidMessageException("Destination must not be blank.");
      }
      if (iface != null && iface.getDelegate().isBlank()) {
        throw new InvalidMessageException("Interface name must not be blank.");
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
