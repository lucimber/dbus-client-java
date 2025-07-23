/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Simple Authentication and Security Layer (SASL) implementation for D-Bus authentication.
 *
 * <p>This package provides core SASL types and data structures used by the D-Bus authentication
 * system. The actual SASL authentication implementation is handled by the 
 * {@link com.lucimber.dbus.netty.sasl} package.
 *
 * <h2>Core SASL Types</h2>
 *
 * <h3>SaslMessage</h3>
 * <p>Represents SASL protocol messages exchanged during authentication:
 * <pre>{@code
 * SaslMessage authMessage = SaslMessage.create(
 *     SaslCommandName.AUTH,
 *     "EXTERNAL",
 *     "31303030"  // UID as hex
 * );
 * }</pre>
 *
 * <h3>SaslCommandName</h3>
 * <p>Enumeration of SASL command types:
 * <ul>
 *   <li><strong>AUTH:</strong> Authentication initiation
 *   <li><strong>CANCEL:</strong> Authentication cancellation  
 *   <li><strong>DATA:</strong> Authentication data exchange
 *   <li><strong>BEGIN:</strong> Start message protocol
 *   <li><strong>REJECTED:</strong> Authentication rejected
 *   <li><strong>OK:</strong> Authentication successful
 *   <li><strong>ERROR:</strong> Protocol error
 * </ul>
 *
 * <h3>SaslAuthMechanism</h3>
 * <p>Enumeration of supported authentication mechanisms:
 * <ul>
 *   <li><strong>EXTERNAL:</strong> Unix credentials authentication
 *   <li><strong>COOKIE:</strong> Cookie-based authentication (DBUS_COOKIE_SHA1)
 *   <li><strong>ANONYMOUS:</strong> Anonymous authentication
 * </ul>
 *
 * <h2>Authentication Process</h2>
 *
 * <p>The SASL authentication follows the D-Bus specification:
 *
 * <ol>
 *   <li><strong>Mechanism Selection:</strong> Client proposes authentication mechanism
 *   <li><strong>Challenge Exchange:</strong> Server may send challenges, client responds
 *   <li><strong>Authentication Result:</strong> Server sends OK or REJECTED
 *   <li><strong>Protocol Start:</strong> Client sends BEGIN to start D-Bus message protocol
 * </ol>
 *
 * <pre>{@code
 * // Typical SASL flow:
 * // Client -> Server: AUTH EXTERNAL 31303030
 * // Server -> Client: OK b25c0b89b8f9b4e9d2a8c4f3e7d6b1a0
 * // Client -> Server: BEGIN
 * }</pre>
 *
 * <h2>Implementation Notes</h2>
 *
 * <p>This package contains data types and constants only. The actual SASL authentication
 * logic is implemented in:
 *
 * <ul>
 *   <li>{@link com.lucimber.dbus.netty.sasl.SaslAuthenticationHandler} - Main authentication handler
 *   <li>{@link com.lucimber.dbus.netty.sasl.SaslMechanism} - Authentication mechanism implementations  
 *   <li>{@link com.lucimber.dbus.netty.sasl.SaslCodec} - SASL message encoding/decoding
 * </ul>
 *
 * <p>Authentication is performed automatically during connection establishment. No manual
 * SASL configuration is required for standard D-Bus connections.
 *
 * @see com.lucimber.dbus.netty.sasl
 * @see com.lucimber.dbus.connection.Connection
 * @see com.lucimber.dbus.netty.NettyConnection
 * @since 2.0
 */
package com.lucimber.dbus.connection.sasl;