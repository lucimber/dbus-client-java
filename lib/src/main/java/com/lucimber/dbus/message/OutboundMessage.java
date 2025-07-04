/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;
import java.util.Optional;

/**
 * An outbound message is a message that should be sent from this service to another service
 * on the same bus that this service is connected to.
 *
 * @since 1.0
 */
public interface OutboundMessage extends Message {

  /**
   * Gets the unique name of the destination.
   *
   * @return an {@link Optional} of {@link DBusString}
   */
  Optional<DBusString> getDestination();
}
