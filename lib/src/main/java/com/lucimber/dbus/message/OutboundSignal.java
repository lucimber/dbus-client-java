/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import java.util.List;
import java.util.Optional;

/**
 * An outbound signal message.
 *
 * @since 1.0
 */
public final class OutboundSignal extends AbstractSignal implements OutboundMessage {

  private final DBusString dst;

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial    the serial number
   * @param path      the object path
   * @param iface     the name of the interface
   * @param member    the name of this signal
   * @param dst       optional; the destination of this signal
   * @param signature optional; the signature of the message body
   * @param payload   optional; the message body
   */
  public OutboundSignal(
          DBusUInt32 serial,
          DBusObjectPath path,
          DBusString iface,
          DBusString member,
          DBusString dst,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, path, iface, member, signature, payload);
    this.dst = dst;
  }

  @Override
  public Optional<DBusString> getDestination() {
    return Optional.ofNullable(dst);
  }

  @Override
  public String toString() {
    var s = "OutboundSignal{dst='%s', serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var dst = getDestination().map(DBusString::toString).orElse("");
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, dst, getSerial(), getObjectPath(), getInterfaceName(), getMember(), sig);
  }

  public static class Builder {
    private DBusUInt32 serial;
    private DBusObjectPath path;
    private DBusString iface;
    private DBusString member;
    private DBusString dst;
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

    public Builder withObjectPath(DBusObjectPath path) {
      this.path = path;
      return this;
    }

    public Builder withInterface(DBusString iface) {
      this.iface = iface;
      return this;
    }

    public Builder withMember(DBusString member) {
      this.member = member;
      return this;
    }

    public Builder withDestination(DBusString dst) {
      this.dst = dst;
      return this;
    }

    public Builder withBody(DBusSignature signature, List<? extends DBusType> payload) {
      this.signature = signature;
      this.payload = payload;
      return this;
    }

    public OutboundSignal build() {
      validate();
      return new OutboundSignal(
              serial,
              path,
              iface,
              member,
              dst,
              signature,
              payload
      );
    }

    private void validate() {
      if (serial == null) {
        throw new InvalidMessageException("Serial must not be null.");
      }
      if (path == null) {
        throw new InvalidMessageException("Object path must not be null.");
      }
      if (iface == null) {
        throw new InvalidMessageException("Interface name must not be null.");
      } else if (iface.getDelegate().isBlank()) {
        throw new InvalidMessageException("Interface name must not be blank.");
      }
      if (member == null) {
        throw new InvalidMessageException("Member name must not be null.");
      } else if (member.getDelegate().isBlank()) {
        throw new InvalidMessageException("Member name must not be blank.");
      }
      if (dst != null && dst.getDelegate().isBlank()) {
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
