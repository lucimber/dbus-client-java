/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusString;

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
   * @return a {@link DBusString}
   */
  DBusString getSender();
}
