/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Protocol and serialization context for the D-Bus client.
 *
 * <p>This package defines:</p>
 * <ul>
 *   <li>The D-Bus message model (headers and bodies).</li>
 *   <li>Marshalling and unmarshalling of messages according to the D-Bus type system.</li>
 *   <li>Byte order, alignment, and padding logic.</li>
 *   <li>Factories for constructing and parsing message headers.</li>
 * </ul>
 *
 * @since 2.0
 */
package com.lucimber.dbus.protocol;