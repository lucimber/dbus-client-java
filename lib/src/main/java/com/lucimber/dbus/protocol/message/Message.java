/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusSignature;
import com.lucimber.dbus.protocol.types.BetterDBusType;
import com.lucimber.dbus.protocol.types.BetterDBusUInt32;

import java.util.List;
import java.util.Optional;

/**
 * This interface declares the common properties of the different D-Bus message types.
 * A concrete D-Bus message must implement this interface.
 *
 * @since 1.0
 */
public interface Message {

  /**
   * Gets the serial number of this D-Bus message.
   *
   * @return an {@link BetterDBusUInt32}
   */
  BetterDBusUInt32 getSerial();

  /**
   * Sets the serial number of this D-Bus message.
   * Must not be {@code null}.
   *
   * @param serial an {@link BetterDBusUInt32}
   */
  void setSerial(BetterDBusUInt32 serial);

  /**
   * Gets the payload of this D-Bus message.
   *
   * @return a {@link List} of {@link BetterDBusType}s.
   */
  List<BetterDBusType> getPayload();

  /**
   * Sets the payload of this D-Bus message.
   * The payload can only be set after a signature has been set before.
   *
   * @param payload a {@link List} of {@link BetterDBusType}s.
   * @see BetterDBusSignature
   */
  void setPayload(List<? extends BetterDBusType> payload);

  /**
   * Gets the signature of the payload.
   *
   * @return an {@link Optional} of {@link BetterDBusSignature}
   */
  Optional<BetterDBusSignature> getSignature();

  /**
   * Sets the signature of the payload.
   * The payload can only be set after a signature has been set before.
   *
   * @param signature a {@link BetterDBusSignature}
   */
  void setSignature(BetterDBusSignature signature);
}
