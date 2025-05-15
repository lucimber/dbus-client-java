/*
 * SPDX-FileCopyrightText: 2023 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.protocol.visitor;

import static org.junit.jupiter.api.Assertions.*;

import com.lucimber.dbus.protocol.factory.DBusTypeFactory;
import com.lucimber.dbus.protocol.signature.SignatureParser;
import com.lucimber.dbus.protocol.signature.ast.TypeDescriptor;
import com.lucimber.dbus.protocol.types.*;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unit tests for WriteVisitor and ReadVisitor round-trip serialization.
 *
 * @since 2.0
 */
class DbusTypeVisitorTest {

  private static final ByteOrder ORDER = ByteOrder.LITTLE_ENDIAN;

  private BetterDBusType roundTrip(final String sig, final Object rawValue) {
    // Parse signature
    TypeDescriptor desc = new SignatureParser(sig).parse();
    // Wrap raw value
    BetterDBusType original = DBusTypeFactory.from(desc, rawValue);
    // Write to buffer
    WriteVisitor writer = new WriteVisitor(ORDER);
    ByteBuffer buf = original.accept(writer);
    // Prepare for read
    buf.order(ORDER);
    // Read from buffer
    ReadVisitor reader = new ReadVisitor(buf, ORDER);
    return reader.read(desc);
  }

  @Test
  void testBasicInt32() {
    BetterDBusType result = roundTrip("i", 0x11223344);
    assertInstanceOf(BetterDBusInt32.class, result);
    assertEquals(0x11223344, ((BetterDBusInt32) result).value());
  }

  @Test
  void testBasicString() {
    BetterDBusType result = roundTrip("s", "hello");
    assertInstanceOf(BetterDBusString.class, result);
    assertEquals("hello", ((BetterDBusString) result).value());
  }

  @Test
  void testBasicObjectPath() {
    BetterDBusType result = roundTrip("o", "/org/example/Service");
    assertInstanceOf(BetterDBusObjectPath.class, result);
    assertEquals("/org/example/Service", ((BetterDBusObjectPath) result).path());
  }

  @Test
  void testBasicSignature() {
    BetterDBusType result = roundTrip("g", "a{is}");
    assertInstanceOf(BetterDBusSignature.class, result);
    assertEquals("a{is}", ((BetterDBusSignature) result).getValue());
  }

  @Test
  void testUint64() {
    BetterDBusType result = roundTrip("t", BigInteger.valueOf(0x12345678abcdef01L));
    assertInstanceOf(BetterDBusUInt64.class, result);
    assertEquals(new BigInteger("12345678abcdef01", 16), ((BetterDBusUInt64) result).value());
  }

  @Test
  void testArrayOfInt() {
    List<Integer> raw = List.of(1, 2, 3);
    BetterDBusArray arr = (BetterDBusArray) roundTrip("ai", raw);
    assertEquals(3, arr.elements().size());
    assertEquals(List.of(1,2,3), arr.elements().stream()
          .map(e -> ((BetterDBusInt32)e).value()).toList());
  }

  @Test
  void testStruct() {
    List<Object> raw = List.of(5, true);
    BetterDBusStruct struct = (BetterDBusStruct) roundTrip("(ib)", raw);
    assertEquals(2, struct.members().size());
    assertEquals(5, ((BetterDBusInt32)struct.members().get(0)).value());
    assertTrue(((BetterDBusBoolean) struct.members().get(1)).value());
  }

  @Test
  void testDictArray() {
    Map<Integer, String> rawMap = Map.of(1, "one", 2, "two");
    BetterDBusArray arr = (BetterDBusArray) roundTrip("a{is}", rawMap);
    assertEquals(2, arr.elements().size());
    Map<BetterDBusInt32, BetterDBusString> map = arr.elements().stream()
          .map(e -> (BetterDBusDictEntry)e)
          .collect(Collectors.toMap(
                de -> (BetterDBusInt32)de.key(),
                de -> (BetterDBusString)de.value()
          ));
    assertEquals("one", map.get(new BetterDBusInt32(1)).value());
    assertEquals("two", map.get(new BetterDBusInt32(2)).value());
  }

  @Test
  void testVariant() {
    BetterDBusType nested = new BetterDBusString("foo");
    BetterDBusVariant var = (BetterDBusVariant) roundTrip("v", nested);
    assertInstanceOf(BetterDBusString.class, var.value());
    assertEquals("foo", ((BetterDBusString)var.value()).value());
  }
}
