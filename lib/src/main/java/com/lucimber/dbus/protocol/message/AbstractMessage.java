/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusSignature;
import com.lucimber.dbus.protocol.types.BetterDBusType;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.*;

/**
 * @since 1.0
 */
abstract class AbstractMessage implements Message {

  private BetterDBusUInt32 serial;
  private List<BetterDBusType> payload;
  private BetterDBusSignature signature;

  AbstractMessage(BetterDBusUInt32 serial) {
    this.serial = Objects.requireNonNull(serial);
  }

  @Override
  public BetterDBusUInt32 getSerial() {
    return serial;
  }

  @Override
  public void setSerial(BetterDBusUInt32 serial) {
    this.serial = Objects.requireNonNull(serial);
  }

  @Override
  public List<BetterDBusType> getPayload() {
    return payload == null ? Collections.emptyList() : new ArrayList<>(payload);
  }

  @Override
  public void setPayload(List<? extends BetterDBusType> payload) {
    this.payload = (payload == null) ? null : new ArrayList<>(payload);
  }

  @Override
  public Optional<BetterDBusSignature> getSignature() {
    return Optional.ofNullable(signature);
  }

  @Override
  public void setSignature(BetterDBusSignature signature) {
    this.signature = signature;
  }

  @Override
  public String toString() {
    var s = "AbstractMessage{serial=%s, signature=%s}";
    return String.format(s, serial, getSignature());
  }
}
