/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.message;

import com.lucimber.dbus.type.DBusUInt32;

/**
 * An inbound reply to a previously send message.
 *
 * @since 1.0
 */
public interface InboundReply extends InboundMessage {

    /**
     * Gets the serial number of the message this message is a reply to.
     *
     * @return The serial number as an {@link DBusUInt32}.
     */
    DBusUInt32 getReplySerial();
}
