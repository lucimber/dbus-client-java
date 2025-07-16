/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.type;

/**
 * Represents the alignment values of the data types that are used by D-Bus.
 */
public enum TypeAlignment {

  BYTE(1),
  BOOLEAN(4),
  INT16(2),
  UINT16(2),
  INT32(4),
  UINT32(4),
  INT64(8),
  UINT64(8),
  DOUBLE(8),
  STRING(4),
  OBJECT_PATH(4),
  SIGNATURE(1),
  ARRAY(4),
  STRUCT(8),
  VARIANT(1),
  DICT_ENTRY(8),
  UNIX_FD(4);

  private final int alignment;

  TypeAlignment(final int alignment) {
    this.alignment = alignment;
  }

  /**
   * Gets the number of bytes that are necessary for the alignment in the wire format.
   *
   * @return an {@link Integer}
   */
  public int getAlignment() {
    return alignment;
  }
}
