/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.codec.encoder;

import com.lucimber.dbus.type.DBusSignature;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SignatureEncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";
  private static final String COMPLEX_VALID_SIGNATURE = "a(bi)aaa{sb}uv(bh(ig))(qat)v";
  private static final String INVALID_BRACKET_SIGNATURE = "a(ia(ii)";
  private static final String INVALID_CHAR_SIGNATURE = "z(i)";
  private static final String VALID_SIGNATURE = "a(ii)";

  @Test
  void encodeValidSignature() {
  DBusSignature signature = DBusSignature.valueOf(VALID_SIGNATURE);
  Encoder<DBusSignature, ByteBuffer> encoder = new SignatureEncoder();
  EncoderResult<ByteBuffer> result = encoder.encode(signature, 0);
  int expectedBytes = 7;
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  int length = VALID_SIGNATURE.length();
  assertEquals(length, buffer.get(0), "Signature length");
  assertEquals((byte) 0x00, buffer.get(length + 1), "Trailing NUL byte");
  }

  @Test
  void encodeValidComplexSignature() {
  DBusSignature signature = DBusSignature.valueOf(COMPLEX_VALID_SIGNATURE);
  Encoder<DBusSignature, ByteBuffer> encoder = new SignatureEncoder();
  EncoderResult<ByteBuffer> result = encoder.encode(signature, 0);
  int expectedBytes = 30;
  assertEquals(expectedBytes, result.getProducedBytes(), PRODUCED_BYTES);
  ByteBuffer buffer = result.getBuffer();
  assertEquals(expectedBytes, buffer.remaining(), READABLE_BYTES);
  int length = COMPLEX_VALID_SIGNATURE.length();
  assertEquals(length, buffer.get(0), "Signature length");
  assertEquals((byte) 0x00, buffer.get(length + 1), "Trailing NUL byte");
  }

  @Test
  void failDueToInvalidChar() {
  Encoder<DBusSignature, ByteBuffer> encoder = new SignatureEncoder();
  assertThrows(Exception.class, () -> {
      DBusSignature signature = DBusSignature.valueOf(INVALID_CHAR_SIGNATURE);
      encoder.encode(signature, 0);
  });
  }

  @Test
  void failDueToInvalidBracketCount() {
  Encoder<DBusSignature, ByteBuffer> encoder = new SignatureEncoder();
  assertThrows(Exception.class, () -> {
      DBusSignature signature = DBusSignature.valueOf(INVALID_BRACKET_SIGNATURE);
      encoder.encode(signature, 0);
  });
  }
}
