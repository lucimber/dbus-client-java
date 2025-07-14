/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
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
   * @return an {@link DBusUInt32}
   */
  DBusUInt32 getSerial();

  /**
   * Gets the payload of this D-Bus message.
   *
   * @return a {@link List} of {@link DBusType}s.
   */
  List<DBusType> getPayload();

  /**
   * Gets the signature of the payload.
   *
   * @return an {@link Optional} of {@link DBusSignature}
   */
  Optional<DBusSignature> getSignature();
}
