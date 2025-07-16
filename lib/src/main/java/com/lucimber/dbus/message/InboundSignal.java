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
import java.util.Objects;

/**
 * An inbound signal message.
 *
 * @since 1.0
 */
public final class InboundSignal extends AbstractSignal implements InboundMessage {

  private final DBusString sender;

  /**
   * Constructs a new instance with mandatory parameter.
   *
   * @param serial the serial number
   * @param sender the sender of this signal
   * @param path   the object path
   * @param iface  the name of the D-Bus interface
   * @param member the name of this signal
   */
  public InboundSignal(
          DBusUInt32 serial,
          DBusString sender,
          DBusObjectPath path,
          DBusString iface,
          DBusString member) {
    super(serial, path, iface, member);
    this.sender = Objects.requireNonNull(sender);
  }

  /**
   * Constructs a new instance with all parameter.
   *
   * @param serial    the serial number
   * @param sender    the sender of this signal
   * @param path      the object path
   * @param iface     the name of the D-Bus interface
   * @param member    the name of this signal
   * @param signature optional; the signature of the message body
   * @param payload   optional; the message body
   */
  public InboundSignal(
          DBusUInt32 serial,
          DBusString sender,
          DBusObjectPath path,
          DBusString iface,
          DBusString member,
          DBusSignature signature,
          List<? extends DBusType> payload) {
    super(serial, path, iface, member, signature, payload);
    this.sender = Objects.requireNonNull(sender);
  }

  @Override
  public DBusString getSender() {
    return sender;
  }

  @Override
  public String toString() {
    var s = "InboundSignal{sender='%s', serial='%s', path='%s', iface='%s', member='%s', sig='%s'}";
    var sig = getSignature().map(DBusSignature::toString).orElse("");
    return String.format(s, sender, getSerial(), getObjectPath(), getInterfaceName(), getMember(), sig);
  }

  public static class Builder {
    private DBusUInt32 serial;
    private DBusString sender;
    private DBusObjectPath path;
    private DBusString iface;
    private DBusString member;
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

    public Builder withSender(DBusString sender) {
      this.sender = sender;
      return this;
    }

    public Builder withObjectPath(DBusObjectPath path) {
      this.path = path;
      return this;
    }

    public Builder withInterfaceName(DBusString iface) {
      this.iface = iface;
      return this;
    }

    public Builder withMember(DBusString member) {
      this.member = member;
      return this;
    }

    public Builder withBody(DBusSignature signature, List<? extends DBusType> payload) {
      this.signature = signature;
      this.payload = payload;
      return this;
    }

    public InboundSignal build() {
      validate();
      return new InboundSignal(
              serial,
              sender,
              path,
              iface,
              member,
              signature,
              payload
      );
    }

    private void validate() {
      if (serial == null) {
        throw new InvalidMessageException("Serial must not be null.");
      }
      if (sender == null) {
        throw new InvalidMessageException("Sender must not be null.");
      } else if (sender.getDelegate().isBlank()) {
        throw new InvalidMessageException("Sender must not be blank.");
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
        throw new InvalidMessageException("Member must not be null.");
      } else if (member.getDelegate().isBlank()) {
        throw new InvalidMessageException("Member must not be blank.");
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
