/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.encoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.netty.ByteOrder;
import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.Dict;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt16;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class DictionaryEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeEmptySimpleDictionary(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("a{sb}");
    final DictEncoder<DBusString, DBusBoolean> encoder =
            new DictEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final Dict<DBusString, DBusBoolean> dict = new Dict<>(signature);
    final EncoderResult<ByteBuf> result = encoder.encode(dict, 0);
    // Array length + struct boundary
    final int expectedBytes = 4 + 4;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeEmptySimpleDictionaryWithOffset(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("a{sb}");
    final DictEncoder<DBusString, DBusBoolean> encoder =
            new DictEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final Dict<DBusString, DBusBoolean> dict = new Dict<>(signature);
    final int offset = 5;
    final EncoderResult<ByteBuf> result = encoder.encode(dict, offset);
    // Padding + Array length + struct boundary
    final int expectedBytes = 3 + 4 + 4;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeEmptyComplexDictionary(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("a{oa{sv}}");
    final DictEncoder<ObjectPath, Dict<DBusString, Variant>> encoder =
            new DictEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final Dict<ObjectPath, Dict<DBusString, Variant>> dict = new Dict<>(signature);
    final EncoderResult<ByteBuf> result = encoder.encode(dict, 0);
    // Array length + struct boundary
    final int expectedBytes = 4 + 4;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }

  @ParameterizedTest
  @EnumSource(ByteOrder.class)
  void encodeComplexDictionary(final ByteOrder byteOrder) {
    final Signature signature = Signature.valueOf("a{oa{sa{sv}}}");
    final Signature innerDictSignature = Signature.valueOf("a{sv}");
    final Dict<ObjectPath, Dict<DBusString, Dict<DBusString, Variant>>> dict =
            new Dict<>(signature);
    final ObjectPath sPath = ObjectPath.valueOf("/services/A8E7E8EF9629/s0");
    final Signature sSignature = Signature.valueOf("a{sa{sv}}");
    final Dict<DBusString, Dict<DBusString, Variant>> sDict = new Dict<>(sSignature);
    // String of interface and empty dict
    final DBusString iProperties = DBusString.valueOf("org.freedesktop.DBus.Properties");
    final Dict<DBusString, Variant> emptyDict = new Dict<>(innerDictSignature);
    sDict.put(iProperties, emptyDict);
    // String of interface and dict with content
    final DBusString iGattService = DBusString.valueOf("org.bluez.GattService1");
    final Dict<DBusString, Variant> innerDict = new Dict<>(innerDictSignature);
    innerDict.put(DBusString.valueOf("Handle"), Variant.valueOf(UInt16.valueOf((short) 1)));
    innerDict.put(DBusString.valueOf("Primary"), Variant.valueOf(DBusBoolean.valueOf(true)));
    innerDict.put(DBusString.valueOf("UUID"), Variant.valueOf(
            DBusString.valueOf("0000180a-0000-1000-8000-00805f9b34fb")));
    sDict.put(iGattService, innerDict);
    dict.put(sPath, sDict);
    final DictEncoder<ObjectPath, Dict<DBusString, Dict<DBusString, Variant>>> encoder =
            new DictEncoder<>(ByteBufAllocator.DEFAULT, byteOrder, signature);
    final EncoderResult<ByteBuf> result = encoder.encode(dict, 0);
    final int expectedBytes = 216;
    assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
    final ByteBuf buffer = result.getBuffer();
    assertEquals(expectedBytes, buffer.readableBytes(), READABLE_BYTES);
    buffer.release();
  }
}
