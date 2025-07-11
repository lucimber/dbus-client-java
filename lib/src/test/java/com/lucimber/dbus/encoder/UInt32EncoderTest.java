/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusUInt32;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class UInt32EncoderTest {

  static final String PRODUCED_BYTES = "Number of produced bytes";
  static final String READABLE_BYTES = "Number of readable bytes";

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedIntegerMaxValue(ByteOrder byteOrder) {
    Encoder<DBusUInt32, ByteBuffer> encoder = new UInt32Encoder(byteOrder);
    DBusUInt32 uint32 = DBusUInt32.valueOf(-1); // 0xFFFFFFFF as unsigned
    EncoderResult<ByteBuffer> result = encoder.encode(uint32, 0);
    int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    
    byte[] expectedBytes;
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expectedBytes = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    } else {
      expectedBytes = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    }
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, byteOrder + " byte order");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedIntegerMinValue(ByteOrder byteOrder) {
    Encoder<DBusUInt32, ByteBuffer> encoder = new UInt32Encoder(byteOrder);
    DBusUInt32 uint32 = DBusUInt32.valueOf(0);
    EncoderResult<ByteBuffer> result = encoder.encode(uint32, 0);
    int expectedNumOfBytes = 4;
    assertEquals(expectedNumOfBytes, result.getProducedBytes(), PRODUCED_BYTES);
    ByteBuffer buffer = result.getBuffer();
    assertEquals(expectedNumOfBytes, buffer.remaining(), READABLE_BYTES);
    
    byte[] expectedBytes = {0x00, 0x00, 0x00, 0x00};
    byte[] actualBytes = new byte[expectedNumOfBytes];
    buffer.get(0, actualBytes);
    assertArrayEquals(expectedBytes, actualBytes, byteOrder + " byte order");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedIntegerWithMultipleOffsets(ByteOrder byteOrder) {
    DBusUInt32 uint32 = DBusUInt32.valueOf((int) 0x12345678L);
    Encoder<DBusUInt32, ByteBuffer> encoder = new UInt32Encoder(byteOrder);
    
    // Test with offset 0 - no padding needed
    EncoderResult<ByteBuffer> result0 = encoder.encode(uint32, 0);
    assertEquals(4, result0.getProducedBytes(), "Offset 0: " + PRODUCED_BYTES);
    byte[] expected0, actual0 = new byte[4];
    result0.getBuffer().get(0, actual0);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected0 = new byte[]{0x12, 0x34, 0x56, 0x78};
    } else {
      expected0 = new byte[]{0x78, 0x56, 0x34, 0x12};
    }
    assertArrayEquals(expected0, actual0, byteOrder + " Offset 0");
    
    // Test with offset 1 - requires 3 bytes padding to reach 4-byte boundary
    EncoderResult<ByteBuffer> result1 = encoder.encode(uint32, 1);
    assertEquals(7, result1.getProducedBytes(), "Offset 1: " + PRODUCED_BYTES);
    byte[] expected1, actual1 = new byte[7];
    result1.getBuffer().get(0, actual1);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected1 = new byte[]{0x00, 0x00, 0x00, 0x12, 0x34, 0x56, 0x78};
    } else {
      expected1 = new byte[]{0x00, 0x00, 0x00, 0x78, 0x56, 0x34, 0x12};
    }
    assertArrayEquals(expected1, actual1, byteOrder + " Offset 1");
    
    // Test with offset 2 - requires 2 bytes padding to reach 4-byte boundary
    EncoderResult<ByteBuffer> result2 = encoder.encode(uint32, 2);
    assertEquals(6, result2.getProducedBytes(), "Offset 2: " + PRODUCED_BYTES);
    byte[] expected2, actual2 = new byte[6];
    result2.getBuffer().get(0, actual2);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected2 = new byte[]{0x00, 0x00, 0x12, 0x34, 0x56, 0x78};
    } else {
      expected2 = new byte[]{0x00, 0x00, 0x78, 0x56, 0x34, 0x12};
    }
    assertArrayEquals(expected2, actual2, byteOrder + " Offset 2");
    
    // Test with offset 3 - requires 1 byte padding to reach 4-byte boundary
    EncoderResult<ByteBuffer> result3 = encoder.encode(uint32, 3);
    assertEquals(5, result3.getProducedBytes(), "Offset 3: " + PRODUCED_BYTES);
    byte[] expected3, actual3 = new byte[5];
    result3.getBuffer().get(0, actual3);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected3 = new byte[]{0x00, 0x12, 0x34, 0x56, 0x78};
    } else {
      expected3 = new byte[]{0x00, 0x78, 0x56, 0x34, 0x12};
    }
    assertArrayEquals(expected3, actual3, byteOrder + " Offset 3");
    
    // Test with offset 4 - no padding needed, already at 4-byte boundary
    EncoderResult<ByteBuffer> result4 = encoder.encode(uint32, 4);
    assertEquals(4, result4.getProducedBytes(), "Offset 4: " + PRODUCED_BYTES);
    byte[] expected4, actual4 = new byte[4];
    result4.getBuffer().get(0, actual4);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected4 = new byte[]{0x12, 0x34, 0x56, 0x78};
    } else {
      expected4 = new byte[]{0x78, 0x56, 0x34, 0x12};
    }
    assertArrayEquals(expected4, actual4, byteOrder + " Offset 4");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedIntegerBoundaryValues(ByteOrder byteOrder) {
    Encoder<DBusUInt32, ByteBuffer> encoder = new UInt32Encoder(byteOrder);
    
    // Test with value 1
    DBusUInt32 one = DBusUInt32.valueOf(1);
    EncoderResult<ByteBuffer> result1 = encoder.encode(one, 0);
    assertEquals(4, result1.getProducedBytes(), "Value 1: " + PRODUCED_BYTES);
    byte[] expected1, actual1 = new byte[4];
    result1.getBuffer().get(0, actual1);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected1 = new byte[]{0x00, 0x00, 0x00, 0x01};
    } else {
      expected1 = new byte[]{0x01, 0x00, 0x00, 0x00};
    }
    assertArrayEquals(expected1, actual1, byteOrder + " Value 1");
    
    // Test with value 255
    DBusUInt32 value255 = DBusUInt32.valueOf(255);
    EncoderResult<ByteBuffer> result255 = encoder.encode(value255, 0);
    assertEquals(4, result255.getProducedBytes(), "Value 255: " + PRODUCED_BYTES);
    byte[] expected255, actual255 = new byte[4];
    result255.getBuffer().get(0, actual255);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected255 = new byte[]{0x00, 0x00, 0x00, (byte) 0xFF};
    } else {
      expected255 = new byte[]{(byte) 0xFF, 0x00, 0x00, 0x00};
    }
    assertArrayEquals(expected255, actual255, byteOrder + " Value 255");
    
    // Test with value 65535
    DBusUInt32 value65535 = DBusUInt32.valueOf(65535);
    EncoderResult<ByteBuffer> result65535 = encoder.encode(value65535, 0);
    assertEquals(4, result65535.getProducedBytes(), "Value 65535: " + PRODUCED_BYTES);
    byte[] expected65535, actual65535 = new byte[4];
    result65535.getBuffer().get(0, actual65535);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected65535 = new byte[]{0x00, 0x00, (byte) 0xFF, (byte) 0xFF};
    } else {
      expected65535 = new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
    }
    assertArrayEquals(expected65535, actual65535, byteOrder + " Value 65535");
    
    // Test with value 16777215 (0xFFFFFF)
    DBusUInt32 value16777215 = DBusUInt32.valueOf(16777215);
    EncoderResult<ByteBuffer> result16777215 = encoder.encode(value16777215, 0);
    assertEquals(4, result16777215.getProducedBytes(), "Value 16777215: " + PRODUCED_BYTES);
    byte[] expected16777215, actual16777215 = new byte[4];
    result16777215.getBuffer().get(0, actual16777215);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected16777215 = new byte[]{0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    } else {
      expected16777215 = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00};
    }
    assertArrayEquals(expected16777215, actual16777215, byteOrder + " Value 16777215");
  }

  @ParameterizedTest
  @MethodSource("com.lucimber.dbus.TestUtils#byteOrderProvider")
  void encodeUnsignedIntegerHighValues(ByteOrder byteOrder) {
    Encoder<DBusUInt32, ByteBuffer> encoder = new UInt32Encoder(byteOrder);
    
    // Test with value 2^31 (2147483648) - appears as negative in signed int
    DBusUInt32 value2147483648 = DBusUInt32.valueOf(Integer.MIN_VALUE); // -2147483648 as int = 2147483648 as unsigned
    EncoderResult<ByteBuffer> result2147483648 = encoder.encode(value2147483648, 0);
    assertEquals(4, result2147483648.getProducedBytes(), "Value 2147483648: " + PRODUCED_BYTES);
    byte[] expected2147483648, actual2147483648 = new byte[4];
    result2147483648.getBuffer().get(0, actual2147483648);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected2147483648 = new byte[]{(byte) 0x80, 0x00, 0x00, 0x00};
    } else {
      expected2147483648 = new byte[]{0x00, 0x00, 0x00, (byte) 0x80};
    }
    assertArrayEquals(expected2147483648, actual2147483648, byteOrder + " Value 2147483648");
    
    // Test with value 4000000000 (as unsigned, appears as negative int)
    DBusUInt32 value4000000000 = DBusUInt32.valueOf((int) 4000000000L);
    EncoderResult<ByteBuffer> result4000000000 = encoder.encode(value4000000000, 0);
    assertEquals(4, result4000000000.getProducedBytes(), "Value 4000000000: " + PRODUCED_BYTES);
    byte[] expected4000000000, actual4000000000 = new byte[4];
    result4000000000.getBuffer().get(0, actual4000000000);
    if (byteOrder == ByteOrder.BIG_ENDIAN) {
      expected4000000000 = new byte[]{(byte) 0xEE, 0x6B, 0x28, 0x00};
    } else {
      expected4000000000 = new byte[]{0x00, 0x28, 0x6B, (byte) 0xEE};
    }
    assertArrayEquals(expected4000000000, actual4000000000, byteOrder + " Value 4000000000");
  }
}