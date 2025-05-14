/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Security and authentication context for the D-Bus client.
 *
 * <p>This package manages:</p>
 * <ul>
 *   <li>SASL authentication mechanisms (EXTERNAL, COOKIE_SHA1, etc.).</li>
 *   <li>State machine for the AUTH/DATA/OK/BEGIN handshake.</li>
 *   <li>Unix file descriptor negotiation.</li>
 *   <li>Credential storage and rotation (e.g., cookie files).</li>
 * </ul>
 *
 * @since 2.0
 */
package com.lucimber.dbus.security;