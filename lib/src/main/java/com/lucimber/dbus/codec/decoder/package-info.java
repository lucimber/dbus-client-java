/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * D-Bus message decoding infrastructure for converting wire format to Java objects.
 *
 * <p>This package provides decoders for all D-Bus types, handling the unmarshalling of binary data
 * from the D-Bus wire protocol into strongly-typed Java objects. The decoders support both
 * big-endian and little-endian byte ordering as specified in the D-Bus message header.
 *
 * <h2>Getting Started</h2>
 *
 * <p><strong>For first-time users:</strong> This package is primarily used internally by the
 * framework. Most users won't interact with decoders directly. For understanding D-Bus types, start
 * with {@link com.lucimber.dbus.type} package instead.
 *
 * <p>Key components include:
 *
 * <ul>
 *   <li>{@link com.lucimber.dbus.codec.decoder.Decoder} - Base decoder interface
 *   <li>{@link com.lucimber.dbus.codec.decoder.DecoderFactory} - Factory for creating type-specific
 *       decoders
 *   <li>Type-specific decoders for all D-Bus basic and container types
 *   <li>Proper alignment handling according to D-Bus specification
 * </ul>
 *
 * @see com.lucimber.dbus.type
 * @see com.lucimber.dbus.codec.encoder
 */
package com.lucimber.dbus.codec.decoder;
