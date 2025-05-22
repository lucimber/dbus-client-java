/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.protocol.types;

/**
 * Represents the data types defined by D-Bus. For further information consult the official documentation.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#idm495">D-Bus Specification: Summary of types</a>
 */
public enum Type {

  /**
   * Unsigned 8-bit integer.
   * ASCII type-code is 121 (ASCII 'y').
   */
  BYTE(TypeCode.BYTE, TypeAlignment.BYTE),

  /**
   * Boolean value, 0 is FALSE and 1 is TRUE. Everything else is invalid.
   * ASCII type-code is 98 (ASCII 'b').
   */
  BOOLEAN(TypeCode.BOOLEAN, TypeAlignment.BOOLEAN),

  /**
   * Signed (two's complement) 16-bit integer.
   * Equivalent to Java's short data type.
   * ASCII type-code is 110 (ASCII 'n').
   */
  INT16(TypeCode.INT16, TypeAlignment.INT16),

  /**
   * Unsigned 16-bit integer.
   * ASCII type-code is 113 (ASCII 'q').
   */
  UINT16(TypeCode.UINT16, TypeAlignment.UINT16),

  /**
   * Signed (two's complement) 32-bit integer
   * Equivalent to Java's integer data type.
   * ASCII type-code is 105 (ASCII 'i').
   */
  INT32(TypeCode.INT32, TypeAlignment.INT32),

  /**
   * Unsigned 32-bit integer.
   * ASCII type-code is 117 (ASCII 'u').
   */
  UINT32(TypeCode.UINT32, TypeAlignment.UINT32),

  /**
   * Signed (two's complement) 64-bit integer
   * Equivalent to Java's long data type.
   * ASCII type-code is 120 (ASCII 'x').
   */
  INT64(TypeCode.INT64, TypeAlignment.INT64),

  /**
   * Unsigned 64-bit integer.
   * ASCII type-code is 116 (ASCII 't').
   */
  UINT64(TypeCode.UINT64, TypeAlignment.UINT64),

  /**
   * IEEE 754 double-precision floating point.
   * Equivalent to Java's double data type.
   * ASCII type-code is 100 (ASCII 'd').
   */
  DOUBLE(TypeCode.DOUBLE, TypeAlignment.DOUBLE),

  /**
   * UTF-8 string (must be valid UTF-8).
   * Must be nul terminated and contain no other nul bytes.
   * ASCII type-code is 115 (ASCII 's').
   */
  STRING(TypeCode.STRING, TypeAlignment.STRING),

  /**
   * An object path is a string-like type and represents a name used to refer to an object instance.
   * ASCII type-code is 111 (ASCII 'o').
   */
  OBJECT_PATH(TypeCode.OBJECT_PATH, TypeAlignment.OBJECT_PATH),

  /**
   * A signature is a string-like type and forms a list of single complete types.
   * ASCII type-code is 103 (ASCII 'g').
   */
  SIGNATURE(TypeCode.SIGNATURE, TypeAlignment.SIGNATURE),

  /**
   * An array is a container type and can store a sequence of a single complete type.
   * ASCII type-code is 97 (ASCII 'a').
   */
  ARRAY(TypeCode.ARRAY, TypeAlignment.ARRAY),

  /**
   * A struct is a container type and can store one or many single complete types.
   * ASCII type-codes are 40 (ASCII '(') and 41 (ASCII ')').
   */
  STRUCT(TypeCode.STRUCT_START, TypeAlignment.STRUCT),

  /**
   * A variant is a container type and can store one single complete type.
   * Variant type (the type of the value is part of the value itself).
   * ASCII type-code is 118 (ASCII 'v').
   */
  VARIANT(TypeCode.VARIANT, TypeAlignment.VARIANT),

  /**
   * An entry is a container type and can store a key-value pair consisting of two single complete types.
   * Entries are used in a dict or map (array of key-value pairs).
   * ASCII type-codes are 123 (ASCII '{') and 125 (ASCII '}').
   */
  DICT_ENTRY(TypeCode.DICT_ENTRY_START, TypeAlignment.DICT_ENTRY),

  /**
   * Unsigned 32-bit integer representing an index into an out-of-band array of file descriptors,
   * transferred via some platform-specific mechanism.
   * ASCII type-code is 104 (ASCII 'h').
   */
  UNIX_FD(TypeCode.UNIX_FD, TypeAlignment.UNIX_FD);

  private final TypeAlignment alignment;
  private final TypeCode code;

  Type(final TypeCode code, final TypeAlignment alignment) {
    this.code = code;
    this.alignment = alignment;
  }

  /**
   * Gets the type's related alignment.
   *
   * @return The {@link TypeAlignment}.
   */
  public TypeAlignment getAlignment() {
    return alignment;
  }

  /**
   * Gets the type's related code.
   *
   * @return a {@link TypeCode}
   */
  public TypeCode getCode() {
    return code;
  }
}
