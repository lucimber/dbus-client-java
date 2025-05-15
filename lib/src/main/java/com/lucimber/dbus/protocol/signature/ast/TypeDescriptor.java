/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature.ast;

/**
 * A D-Bus type signature descriptor.
 * <p>
 * Represents one element of a D-Bus signature:
 * basic, array, dict-entry, struct or variant.
 * </p>
 *
 * @since 2.0
 */
public sealed interface TypeDescriptor
      permits Basic, Array, Dict, Struct, Variant {
}
