/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.signature.ast;

/**
 * Descriptor for the D-Bus VARIANT type, denoted by the code 'v'.
 *
 * <p>Only one instance exists.</p>
 *
 * @since 2.0
 */
public final class Variant implements TypeDescriptor {

  /**
   * Singleton instance of the variant descriptor.
   */
  public static final Variant INSTANCE = new Variant();

  private Variant() {
    // singleton
  }
}