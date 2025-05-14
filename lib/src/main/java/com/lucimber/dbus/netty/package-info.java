/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Transport context using Netty for the D-Bus client.
 *
 * <p>This package handles all low-level network operations, including:</p>
 * <ul>
 *   <li>Bootstrap and configuration of Netty's event loop and channels.</li>
 *   <li>Frame encoding and decoding (length-based framing, alignment).</li>
 *   <li>Integration of authentication and protocol handlers into the Netty pipeline.</li>
 *   <li>Connection lifecycle management (connect, reconnect, close).</li>
 * </ul>
 *
 * @since 2.0
 */
package com.lucimber.dbus.netty;