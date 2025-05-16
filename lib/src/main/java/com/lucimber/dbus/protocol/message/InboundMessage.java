/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.message;

import com.lucimber.dbus.protocol.types.BetterDBusString;

/**
 * An inbound message is a message that got send from another service to this service
 * on the same bus that this service is connected to.
 *
 * @since 1.0
 */
public interface InboundMessage extends Message {

  /**
   * Gets the sender of this inbound message.
   *
   * @return a {@link BetterDBusString}
   */
  BetterDBusString getSender();

  /**
   * Sets the sender of this inbound message.
   * Must not be {@code NULL}.
   *
   * @param sender a {@link BetterDBusString}
   */
  void setSender(BetterDBusString sender);
}
