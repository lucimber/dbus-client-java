/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * D-Bus message decoding infrastructure for converting wire format to Java objects.
 * 
 * <p>This package provides decoders for all D-Bus types, handling the unmarshalling
 * of binary data from the D-Bus wire protocol into strongly-typed Java objects.
 * The decoders support both big-endian and little-endian byte ordering as specified
 * in the D-Bus message header.</p>
 * 
 * <h2>Getting Started</h2>
 * 
 * <p><strong>For first-time users:</strong> This package is primarily used internally by the framework.
 * Most users won't interact with decoders directly. For understanding D-Bus types, start with
 * {@link com.lucimber.dbus.type} package instead.</p>
 * 
 * <p>Key components include:</p>
 * <ul>
 *   <li>{@link com.lucimber.dbus.decoder.Decoder} - Base decoder interface</li>
 *   <li>{@link com.lucimber.dbus.decoder.DecoderFactory} - Factory for creating type-specific decoders</li>
 *   <li>Type-specific decoders for all D-Bus basic and container types</li>
 *   <li>Proper alignment handling according to D-Bus specification</li>
 * </ul>
 * 
 * @see com.lucimber.dbus.type
 * @see com.lucimber.dbus.encoder
 */
package com.lucimber.dbus.decoder;