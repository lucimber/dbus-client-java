/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Provides the core modules of a Netty-based D-Bus client, organized into distinct bounded contexts.
 *
 * <p>The following subpackages define clear responsibilities:</p>
 * <ul>
 *   <li>{@link com.lucimber.dbus.netty} - Transport (Netty-based):
 *       Handles low-level network connectivity, framing, and pipeline initialization.</li>
 *   <li>{@link com.lucimber.dbus.security} - Security/Authentication:
 *       Manages SASL authentication mechanisms and Unix file descriptor negotiation.</li>
 *   <li>{@link com.lucimber.dbus.protocol} - Protocol/Serialization:
 *       Defines the D-Bus message model, marshalling, and unmarshalling logic.</li>
 *   <li>{@link com.lucimber.dbus.api} - High-Level API:
 *       Exposes the DbusConnection interface for method calls, signal handling, and property access.</li>
 *   <li>{@link com.lucimber.dbus.proxy} - Proxy Generator:
 *       Parses introspection XML and creates type-safe client proxies.</li>
 * </ul>
 *
 * @since 1.0
 */
package com.lucimber.dbus;
