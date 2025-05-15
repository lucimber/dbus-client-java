/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.factory;

import com.lucimber.dbus.protocol.signature.ast.*;
import com.lucimber.dbus.protocol.signature.ast.Dict;
import com.lucimber.dbus.protocol.signature.ast.Struct;
import com.lucimber.dbus.protocol.signature.ast.Variant;
import com.lucimber.dbus.protocol.types.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory for creating {@link BetterDBusType} instances from parsed
 * {@link TypeDescriptor descriptors} and raw Java values.
 *
 * <p>This factory handles basic, container, and variant types,
 * recursively constructing the appropriate {@code DbusType} tree.</p>
 *
 * @since 2.0
 */
public final class DBusTypeFactory {

  private DBusTypeFactory() {
    // Utility class
  }

  /**
   * Constructs a {@link BetterDBusType} corresponding to the given
   * {@link TypeDescriptor} and raw Java value.
   *
   * @param descriptor the D-Bus type descriptor
   * @param rawValue   the raw Java value (e.g., Integer, String, List, Map.Entry)
   * @return a {@code DbusType} wrapping the raw value
   * @throws IllegalArgumentException on type mismatches or unsupported descriptors
   */
  public static BetterDBusType from(final TypeDescriptor descriptor, final Object rawValue) {
    if (descriptor instanceof Basic basic) {
      return fromBasic(basic, rawValue);
    } else if (descriptor instanceof Array array) {
      // Special case: array of dict entries from a Map
      if (array.elementType() instanceof Dict dictDesc && rawValue instanceof Map<?, ?> map) {
        List<BetterDBusType> entries = map.entrySet().stream()
              .map(e -> new BetterDBusDictEntry(
                    from(dictDesc.keyType(), e.getKey()),
                    from(dictDesc.valueType(), e.getValue())
              ))
              .collect(Collectors.toList());
        return new BetterDBusArray(entries);
      }
      if (!(rawValue instanceof List<?> list)) {
        throw new IllegalArgumentException(
              "Expected a List for array type, got: " + rawValue.getClass());
      }
      List<BetterDBusType> elements = new ArrayList<>();
      for (Object elem : list) {
        elements.add(from(array.elementType(), elem));
      }
      return new BetterDBusArray(elements);
    } else if (descriptor instanceof Dict dict) {
      // Dict entries: accept Map.Entry or two-element List
      if (rawValue instanceof Map.Entry<?, ?> entry) {
        return new BetterDBusDictEntry(
              from(dict.keyType(), entry.getKey()),
              from(dict.valueType(), entry.getValue())
        );
      } else if (rawValue instanceof List<?> list && list.size() == 2) {
        return new BetterDBusDictEntry(
              from(dict.keyType(), list.get(0)),
              from(dict.valueType(), list.get(1))
        );
      }
      throw new IllegalArgumentException(
            "Expected Map.Entry or List of size 2 for dict entry, got: " +
                  rawValue.getClass());
    } else if (descriptor instanceof Struct struct) {
      if (!(rawValue instanceof List<?> list)) {
        throw new IllegalArgumentException(
              "Expected a List for struct type, got: " + rawValue.getClass());
      }
      List<BetterDBusType> members = new ArrayList<>();
      List<TypeDescriptor> descs = struct.members();
      if (list.size() != descs.size()) {
        throw new IllegalArgumentException(
              "Struct member count mismatch: expected " + descs.size() +
                    ", got " + list.size());
      }
      for (int i = 0; i < descs.size(); i++) {
        members.add(from(descs.get(i), list.get(i)));
      }
      return new BetterDBusStruct(members);
    } else if (descriptor instanceof Variant) {
      if (!(rawValue instanceof BetterDBusType nested)) {
        throw new IllegalArgumentException(
              "Expected a DbusType for variant, got: " +
                    (rawValue == null ? "null" : rawValue.getClass()));
      }
      return new BetterDBusVariant(nested);
    }
    // Should not happen if AST is exhaustive
    throw new IllegalArgumentException(
          "Unsupported TypeDescriptor: " + descriptor.getClass());
  }

  private static BetterDBusType fromBasic(final Basic basic, final Object rawValue) {
    char code = basic.code();
    switch (code) {
      case 'y' -> { // BYTE
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for BYTE, got: " + rawValue.getClass());
        return new BetterDBusByte(num.byteValue());
      }
      case 'b' -> { // BOOLEAN
        if (!(rawValue instanceof Boolean b)) throw new IllegalArgumentException(
              "Expected Boolean for BOOLEAN, got: " + rawValue.getClass());
        return new BetterDBusBoolean(b);
      }
      case 'n' -> { // INT16
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for INT16, got: " + rawValue.getClass());
        return new BetterDBusInt16(num.shortValue());
      }
      case 'q' -> { // UINT16
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for UINT16, got: " + rawValue.getClass());
        return new BetterDBusUInt16(num.intValue());
      }
      case 'i' -> { // INT32
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for INT32, got: " + rawValue.getClass());
        return new BetterDBusInt32(num.intValue());
      }
      case 'u' -> { // UINT32
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for UINT32, got: " + rawValue.getClass());
        return new BetterDBusUInt32(num.longValue());
      }
      case 'x' -> { // INT64
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for INT64, got: " + rawValue.getClass());
        return new BetterDBusInt64(num.longValue());
      }
      case 't' -> { // UINT64
        if (rawValue instanceof BigInteger bi) {
          return new BetterDBusUInt64(bi);
        } else if (rawValue instanceof Number num) {
          return new BetterDBusUInt64(BigInteger.valueOf(num.longValue()));
        }
        throw new IllegalArgumentException(
              "Expected BigInteger or Number for UINT64, got: " + rawValue.getClass());
      }
      case 'd' -> { // DOUBLE
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for DOUBLE, got: " + rawValue.getClass());
        return new BetterDBusDouble(num.doubleValue());
      }
      case 's' -> { // STRING
        if (!(rawValue instanceof String s)) throw new IllegalArgumentException(
              "Expected String for STRING, got: " + rawValue.getClass());
        return new BetterDBusString(s);
      }
      case 'o' -> { // OBJECT_PATH
        if (!(rawValue instanceof String s)) throw new IllegalArgumentException(
              "Expected String for OBJECT_PATH, got: " + rawValue.getClass());
        return new BetterDBusObjectPath(s);
      }
      case 'g' -> { // SIGNATURE
        if (!(rawValue instanceof String s)) throw new IllegalArgumentException(
              "Expected String for SIGNATURE, got: " + rawValue.getClass());
        return new BetterDBusSignature(s);
      }
      case 'h' -> { // UNIX_FD
        if (!(rawValue instanceof Number num)) throw new IllegalArgumentException(
              "Expected Number for UNIX_FD, got: " + rawValue.getClass());
        return new BetterDBusUnixFd(num.intValue());
      }
      default -> throw new IllegalArgumentException(
            "Unknown basic type code: " + code);
    }
  }
}
