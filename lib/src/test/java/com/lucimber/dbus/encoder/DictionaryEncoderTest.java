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
  DBusSignature signature = DBusSignature.valueOf("a{sb}");
  DictEncoder<DBusString, DBusBoolean> encoder =
          new DictEncoder<>(byteOrder, signature);
  DBusDict<DBusString, DBusBoolean> dict = new DBusDict<>(signature);
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
  DBusSignature signature = DBusSignature.valueOf("a{sb}");
  DictEncoder<DBusString, DBusBoolean> encoder =
          new DictEncoder<>(byteOrder, signature);
  DBusDict<DBusString, DBusBoolean> dict = new DBusDict<>(signature);
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
  DBusSignature signature = DBusSignature.valueOf("a{oa{sv}}");
  DictEncoder<DBusObjectPath, DBusDict<DBusString, DBusVariant>> encoder =
          new DictEncoder<>(byteOrder, signature);
  DBusDict<DBusObjectPath, DBusDict<DBusString, DBusVariant>> dict = new DBusDict<>(signature);
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
  DBusSignature signature = DBusSignature.valueOf("a{oa{sa{sv}}}");
  DBusSignature innerDictSignature = DBusSignature.valueOf("a{sv}");
  DBusDict<DBusObjectPath, DBusDict<DBusString, DBusDict<DBusString, DBusVariant>>> dict =
          new DBusDict<>(signature);
  DBusObjectPath sPath = DBusObjectPath.valueOf("/services/A8E7E8EF9629/s0");
  DBusSignature sSignature = DBusSignature.valueOf("a{sa{sv}}");
  DBusDict<DBusString, DBusDict<DBusString, DBusVariant>> sDict = new DBusDict<>(sSignature);
  // String of interface and empty dict
  DBusString iProperties = DBusString.valueOf("org.freedesktop.DBus.Properties");
  DBusDict<DBusString, DBusVariant> emptyDict = new DBusDict<>(innerDictSignature);
  sDict.put(iProperties, emptyDict);
  // String of interface and dict with content
  DBusString iGattService = DBusString.valueOf("org.bluez.GattService1");
  DBusDict<DBusString, DBusVariant> innerDict = new DBusDict<>(innerDictSignature);
  innerDict.put(DBusString.valueOf("Handle"), DBusVariant.valueOf(DBusUInt16.valueOf((short) 1)));
  innerDict.put(DBusString.valueOf("Primary"), DBusVariant.valueOf(DBusBoolean.valueOf(true)));
  innerDict.put(DBusString.valueOf("UUID"), DBusVariant.valueOf(
          DBusString.valueOf("0000180a-0000-1000-8000-00805f9b34fb")));
  sDict.put(iGattService, innerDict);
  dict.put(sPath, sDict);
  DictEncoder<DBusObjectPath, DBusDict<DBusString, DBusDict<DBusString, DBusVariant>>> encoder =
          new DictEncoder<>(byteOrder, signature);
  EncoderResult<ByteBuffer> result = encoder.encode(dict, 0);
  int expectedBytes = 216;
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  }
}
