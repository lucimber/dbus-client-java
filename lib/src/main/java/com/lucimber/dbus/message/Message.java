/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.UInt32;

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
   * @return an {@link UInt32}
   */
  UInt32 getSerial();

  /**
   * Gets the payload of this D-Bus message.
   *
   * @return a {@link List} of {@link DBusType}s.
   */
  List<DBusType> getPayload();

  /**
   * Gets the signature of the payload.
   *
   * @return an {@link Optional} of {@link Signature}
   */
  Optional<Signature> getSignature();
}
