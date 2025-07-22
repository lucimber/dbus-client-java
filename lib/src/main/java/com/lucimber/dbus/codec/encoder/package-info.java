/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * D-Bus message encoding infrastructure for converting Java objects to wire format.
 * 
 * <p>This package provides encoders for all D-Bus types, handling the marshalling
 * of strongly-typed Java objects into the binary D-Bus wire protocol format.
 * The encoders support both big-endian and little-endian byte ordering, with
 * proper alignment and padding according to the D-Bus specification.</p>
 * 
 * <h2>Getting Started</h2>
 * 
 * <p><strong>For first-time users:</strong> This package is primarily used internally by the framework.
 * Most users won't interact with encoders directly. To create D-Bus messages, use the builder
 * classes in {@link com.lucimber.dbus.message} package instead.</p>
 * 
 * <p>Key components include:</p>
 * <ul>
 *   <li>{@link com.lucimber.dbus.codec.encoder.Encoder} - Base encoder interface</li>
 *   <li>{@link com.lucimber.dbus.codec.encoder.EncoderFactory} - Factory for creating type-specific encoders</li>
 *   <li>Type-specific encoders for all D-Bus basic and container types</li>
 *   <li>Automatic alignment and padding calculation</li>
 * </ul>
 * 
 * @see com.lucimber.dbus.type
 * @see com.lucimber.dbus.codec.decoder
 */
package com.lucimber.dbus.codec.encoder;