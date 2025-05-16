/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusString;

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
   * @return an {@link Optional} of {@link BetterDBusString}
   */
  Optional<BetterDBusString> getDestination();

  /**
   * Sets the unique name of the destination.
   *
   * @param destination a {@link BetterDBusString}
   */
  void setDestination(BetterDBusString destination);
}
