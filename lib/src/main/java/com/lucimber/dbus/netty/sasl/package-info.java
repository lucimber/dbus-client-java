/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Netty-specific SASL authentication mechanism implementations.
 * 
 * <p>This package contains the Netty channel handlers and mechanism implementations
 * for D-Bus SASL authentication. It provides concrete implementations of the
 * authentication mechanisms defined in the D-Bus specification:</p>
 * 
 * <ul>
 *   <li>{@link com.lucimber.dbus.netty.sasl.ExternalSaslMechanism} - EXTERNAL mechanism using Unix credentials</li>
 *   <li>{@link com.lucimber.dbus.netty.sasl.CookieSaslMechanism} - DBUS_COOKIE_SHA1 mechanism</li>
 *   <li>{@link com.lucimber.dbus.netty.sasl.AnonymousSaslMechanism} - ANONYMOUS mechanism</li>
 * </ul>
 * 
 * <h2>Getting Started</h2>
 * 
 * <p><strong>For first-time users:</strong> This package handles authentication automatically.
 * You typically won't interact with these classes directly. To configure authentication, use
 * {@link com.lucimber.dbus.connection.sasl.SaslAuthConfig} with your connection configuration.</p>
 * 
 * <p>The authentication process is handled by:</p>
 * <ul>
 *   <li>{@link com.lucimber.dbus.netty.sasl.SaslInitiationHandler} - Initiates SASL handshake</li>
 *   <li>{@link com.lucimber.dbus.netty.sasl.SaslAuthenticationHandler} - Manages authentication state machine</li>
 *   <li>{@link com.lucimber.dbus.netty.sasl.SaslCodec} - Encodes/decodes SASL messages</li>
 * </ul>
 * 
 * @see com.lucimber.dbus.connection.sasl
 * @see com.lucimber.dbus.netty
 */
package com.lucimber.dbus.netty.sasl;