/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.factory;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.protocol.signature.SignatureParser;
import com.lucimber.dbus.protocol.signature.ast.TypeDescriptor;
import com.lucimber.dbus.protocol.types.*;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link DBusTypeFactory}.
 *
 * <p>Verifies correct construction of DbusType instances
 * for basic, container, and variant types, and error handling.</p>
 *
 * @since 2.0
 */
class DBusTypeFactoryTest {

  private TypeDescriptor parse(final String sig) {
    return new SignatureParser(sig).parse();
  }

  @Test
  void testBasicInt32() {
    TypeDescriptor desc = parse("i");
    BetterDBusType dt = DBusTypeFactory.from(desc, 42);
    assertInstanceOf(BetterDBusInt32.class, dt);
    assertEquals(42, ((BetterDBusInt32) dt).value());
  }

  @Test
  void testBasicString() {
    TypeDescriptor desc = parse("s");
    BetterDBusType dt = DBusTypeFactory.from(desc, "hello");
    assertInstanceOf(BetterDBusString.class, dt);
    assertEquals("hello", ((BetterDBusString) dt).value());
  }

  @Test
  void testBasicObjectPath() {
    TypeDescriptor desc = parse("o");
    BetterDBusType dt = DBusTypeFactory.from(desc, "/org/example/Service");
    assertInstanceOf(BetterDBusObjectPath.class, dt);
    assertEquals("/org/example/Service", ((BetterDBusObjectPath) dt).path());
  }

  @Test
  void testBasicSignature() {
    TypeDescriptor desc = parse("g");
    BetterDBusType dt = DBusTypeFactory.from(desc, "a{is}");
    assertInstanceOf(BetterDBusSignature.class, dt);
    // The wrapper's signature() should be 'g' for SIGNATURE type
    assertEquals("g", dt.signature());
    // The contained signature value is the actual D-Bus signature string
    assertEquals("a{is}", ((BetterDBusSignature) dt).getValue());
  }

  @Test
  void testUint64FromNumber() {
    TypeDescriptor desc = parse("t");
    BetterDBusType dt = DBusTypeFactory.from(desc, 123L);
    assertInstanceOf(BetterDBusUInt64.class, dt);
    assertEquals(BigInteger.valueOf(123L), ((BetterDBusUInt64) dt).value());
  }

  @Test
  void testArrayOfInt() {
    TypeDescriptor desc = parse("ai");
    List<Integer> raw = List.of(1, 2, 3);
    BetterDBusType dt = DBusTypeFactory.from(desc, raw);
    assertInstanceOf(BetterDBusArray.class, dt);
    var arr = (BetterDBusArray) dt;
    assertEquals(3, arr.elements().size());
    assertInstanceOf(BetterDBusInt32.class, arr.elements().get(0));
  }

  @Test
  void testDictEntriesFromMap() {
    TypeDescriptor desc = parse("a{is}");
    Map<Integer, String> rawMap = Map.of(1, "one", 2, "two");
    BetterDBusType dt = DBusTypeFactory.from(desc, rawMap);
    assertInstanceOf(BetterDBusArray.class, dt);
    var arr = (BetterDBusArray) dt;
    assertEquals(2, arr.elements().size());
    var entry = arr.elements().get(0);
    assertInstanceOf(BetterDBusDictEntry.class, entry);
    var de = (BetterDBusDictEntry) entry;
    assertInstanceOf(BetterDBusInt32.class, de.key());
    assertInstanceOf(BetterDBusString.class, de.value());
  }

  @Test
  void testStruct() {
    TypeDescriptor desc = parse("(ib)");
    List<Object> raw = List.of(5, true);
    BetterDBusType dt = DBusTypeFactory.from(desc, raw);
    assertInstanceOf(BetterDBusStruct.class, dt);
    var struct = (BetterDBusStruct) dt;
    assertEquals(2, struct.members().size());
    assertInstanceOf(BetterDBusBoolean.class, struct.members().get(1));
  }

  @Test
  void testVariant() {
    TypeDescriptor desc = parse("v");
    BetterDBusType nested = new BetterDBusString("foo");
    BetterDBusType dt = DBusTypeFactory.from(desc, nested);
    assertInstanceOf(BetterDBusVariant.class, dt);
    assertEquals(nested, ((BetterDBusVariant) dt).value());
  }

  @Test
  void testNestedComposite() {
    TypeDescriptor desc = parse("a{iq}");
    Map<Integer, Integer> rawMap = Map.of(1, 100, 2, 200);
    BetterDBusArray arr = (BetterDBusArray) DBusTypeFactory.from(desc, rawMap);
    assertEquals(2, arr.elements().size());
    BetterDBusDictEntry de = (BetterDBusDictEntry) arr.elements().get(1);
    assertEquals(2, ((BetterDBusInt32) de.key()).value());
    assertEquals(200, ((BetterDBusUInt16) de.value()).value());
  }

  @Test
  void testTypeMismatchThrows() {
    TypeDescriptor desc = parse("i");
    assertThrows(IllegalArgumentException.class,
          () -> DBusTypeFactory.from(desc, "not-an-int"));
  }
}
