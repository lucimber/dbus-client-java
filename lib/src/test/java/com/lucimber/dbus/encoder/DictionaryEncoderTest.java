/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DictionaryEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptySimpleDictionary(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("a{sb}");
    DictEncoder<DBusString, DBusBoolean> encoder =
          new DictEncoder<>(byteOrder, signature);
    Dict<DBusString, DBusBoolean> dict = new Dict<>(signature);
    EncoderResult<ByteBuffer> result = encoder.encode(dict, 0);
    // Array length + struct boundary
    int expectedBytes = 4 + 4;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptySimpleDictionaryWithOffset(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("a{sb}");
    DictEncoder<DBusString, DBusBoolean> encoder =
          new DictEncoder<>(byteOrder, signature);
    Dict<DBusString, DBusBoolean> dict = new Dict<>(signature);
    int offset = 5;
    EncoderResult<ByteBuffer> result = encoder.encode(dict, offset);
    // Padding + Array length + struct boundary
    int expectedBytes = 3 + 4 + 4;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeEmptyComplexDictionary(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("a{oa{sv}}");
    DictEncoder<ObjectPath, Dict<DBusString, Variant>> encoder =
          new DictEncoder<>(byteOrder, signature);
    Dict<ObjectPath, Dict<DBusString, Variant>> dict = new Dict<>(signature);
    EncoderResult<ByteBuffer> result = encoder.encode(dict, 0);
    // Array length + struct boundary
    int expectedBytes = 4 + 4;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeComplexDictionary(ByteOrder byteOrder) {
    Signature signature = Signature.valueOf("a{oa{sa{sv}}}");
    Signature innerDictSignature = Signature.valueOf("a{sv}");
    Dict<ObjectPath, Dict<DBusString, Dict<DBusString, Variant>>> dict =
          new Dict<>(signature);
    ObjectPath sPath = ObjectPath.valueOf("/services/A8E7E8EF9629/s0");
    Signature sSignature = Signature.valueOf("a{sa{sv}}");
    Dict<DBusString, Dict<DBusString, Variant>> sDict = new Dict<>(sSignature);
    // String of interface and empty dict
    DBusString iProperties = DBusString.valueOf("org.freedesktop.DBus.Properties");
    Dict<DBusString, Variant> emptyDict = new Dict<>(innerDictSignature);
    sDict.put(iProperties, emptyDict);
    // String of interface and dict with content
    DBusString iGattService = DBusString.valueOf("org.bluez.GattService1");
    Dict<DBusString, Variant> innerDict = new Dict<>(innerDictSignature);
    innerDict.put(DBusString.valueOf("Handle"), Variant.valueOf(UInt16.valueOf((short) 1)));
    innerDict.put(DBusString.valueOf("Primary"), Variant.valueOf(DBusBoolean.valueOf(true)));
    innerDict.put(DBusString.valueOf("UUID"), Variant.valueOf(
          DBusString.valueOf("0000180a-0000-1000-8000-00805f9b34fb")));
    sDict.put(iGattService, innerDict);
    dict.put(sPath, sDict);
    DictEncoder<ObjectPath, Dict<DBusString, Dict<DBusString, Variant>>> encoder =
          new DictEncoder<>(byteOrder, signature);
    EncoderResult<ByteBuffer> result = encoder.encode(dict, 0);
    int expectedBytes = 216;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }
}
