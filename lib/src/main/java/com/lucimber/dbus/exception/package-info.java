/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Standard D-Bus error types and exception mappings.
 *
 * <p>This package contains Java exception classes that correspond to standard D-Bus error names as
 * defined in the D-Bus specification. Each exception represents a specific error condition that can
 * occur during D-Bus communication.
 *
 * <h2>Getting Started</h2>
 *
 * <p><strong>For first-time users:</strong> You'll encounter these exceptions when handling {@link
 * com.lucimber.dbus.message.InboundError} messages. Check for specific exception types to handle
 * different error conditions appropriately in your application.
 *
 * <p>The exceptions follow the D-Bus error naming convention and are automatically mapped to/from
 * D-Bus error messages. Common exceptions include:
 *
 * <ul>
 *   <li>{@link com.lucimber.dbus.exception.UnknownMethodException} - Method does not exist
 *   <li>{@link com.lucimber.dbus.exception.InvalidArgsException} - Invalid method arguments
 *   <li>{@link com.lucimber.dbus.exception.AccessDeniedException} - Access denied by policy
 *   <li>{@link com.lucimber.dbus.exception.TimeoutException} - Operation timed out
 *   <li>{@link com.lucimber.dbus.exception.NoReplyException} - No reply to method call
 * </ul>
 *
 * <p>All exceptions extend {@link com.lucimber.dbus.exception.AbstractException} which provides the
 * D-Bus error name mapping.
 *
 * @see com.lucimber.dbus.message.InboundError
 * @see com.lucimber.dbus.message.OutboundError
 */
package com.lucimber.dbus.exception;
