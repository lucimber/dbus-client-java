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
 * Represents the type codes of the data types that are used by D-Bus.
 * Each type code is represented by a character defined in the encoding standard ASCII.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#idm495">
 * D-Bus Specification: Summary of types</a>
 */
public enum TypeCode {

  BYTE('y'),
  BOOLEAN('b'),
  INT16('n'),
  UINT16('q'),
  INT32('i'),
  UINT32('u'),
  INT64('x'),
  UINT64('t'),
  DOUBLE('d'),
  STRING('s'),
  OBJECT_PATH('o'),
  SIGNATURE('g'),
  ARRAY('a'),
  // STRUCT('r'),
  STRUCT_START('('),
  STRUCT_END(')'),
  VARIANT('v'),
  // DICT_ENTRY('e'),
  DICT_ENTRY_START('{'),
  DICT_ENTRY_END('}'),
  UNIX_FD('h');

  private final char ch;

  TypeCode(final char ch) {
    this.ch = ch;
  }

  /**
   * Gets the char of this type code.
   *
   * @return a {@link Character}
   */
  public char getChar() {
    return ch;
  }
}
