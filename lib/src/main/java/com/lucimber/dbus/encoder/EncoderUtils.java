/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.encoder;

import com.lucimber.dbus.type.DBusArray;
import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusBoolean;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusContainerType;
import com.lucimber.dbus.type.DBusDouble;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Dict;
import com.lucimber.dbus.type.DictEntry;
import com.lucimber.dbus.type.Int16;
import com.lucimber.dbus.type.Int32;
import com.lucimber.dbus.type.Int64;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.TypeAlignment;
import com.lucimber.dbus.type.UInt16;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.UInt64;
import com.lucimber.dbus.type.UnixFd;
import com.lucimber.dbus.type.Variant;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Various methods used by the ByteBuffer-based implementations of the encoders.
 */
public final class EncoderUtils {

  private EncoderUtils() {
    // Utility class
  }

  static int calculateAlignmentPadding(TypeAlignment alignment, int offset) {
    int remainder = offset % alignment.getAlignment();
    return (remainder == 0) ? 0 : (alignment.getAlignment() - remainder);
  }

  /**
   * Encodes a DBusType into its binary representation using ByteBuffer.
   *
   * @param value  the value to encode
   * @param offset the offset where the value will be written
   * @param order  byte order to use
   * @return an EncoderResult with the encoded buffer and byte count
   * @throws EncoderException if the value cannot be encoded
   */
  public static EncoderResult<ByteBuffer> encode(DBusType value, int offset, ByteOrder order)
          throws EncoderException {
    Objects.requireNonNull(value, "value must not be null");
    if (value instanceof DBusContainerType container) {
      return encodeContainerType(container, offset, order);
    } else if (value instanceof DBusBasicType basic) {
      return encodeBasicType(basic, offset, order);
    } else {
      throw new EncoderException("Unknown DBusType: " + value.getClass().getName());
    }
  }

  private static EncoderResult<ByteBuffer> encodeBasicType(DBusBasicType value, int offset, ByteOrder order)
          throws EncoderException {
    if (value instanceof DBusBoolean b) {
      return new BooleanEncoder(order).encode(b, offset);
    } else if (value instanceof DBusByte b) {
      return new ByteEncoder().encode(b, offset);
    } else if (value instanceof DBusDouble d) {
      return new DoubleEncoder(order).encode(d, offset);
    } else if (value instanceof Int16 i16) {
      return new Int16Encoder(order).encode(i16, offset);
    } else if (value instanceof Int32 i32) {
      return new Int32Encoder(order).encode(i32, offset);
    } else if (value instanceof Int64 i64) {
      return new Int64Encoder(order).encode(i64, offset);
    } else if (value instanceof UInt16 u16) {
      return new UInt16Encoder(order).encode(u16, offset);
    } else if (value instanceof UInt32 u32) {
      return new UInt32Encoder(order).encode(u32, offset);
    } else if (value instanceof UInt64 u64) {
      return new UInt64Encoder(order).encode(u64, offset);
    } else if (value instanceof DBusString str) {
      return new StringEncoder(order).encode(str, offset);
    } else if (value instanceof ObjectPath path) {
      return new ObjectPathEncoder(order).encode(path, offset);
    } else if (value instanceof Signature sig) {
      return new SignatureEncoder().encode(sig, offset);
    } else if (value instanceof UnixFd fd) {
      return new UnixFdEncoder(order).encode(fd, offset);
    } else {
      throw new EncoderException("Unsupported basic type: " + value.getClass().getName());
    }
  }

  @SuppressWarnings("unchecked")
  private static EncoderResult<ByteBuffer> encodeContainerType(DBusContainerType value, int offset,
                                                               ByteOrder order) throws EncoderException {
    Signature signature = value.getSignature();
    if (signature.isArray()) {
      return new ArrayEncoder<>(
              order,
              signature
      ).encode((DBusArray<DBusType>) value, offset);
    } else if (signature.isDictionary()) {
      return new DictEncoder<>(
              order,
              signature
      ).encode((Dict<DBusBasicType, DBusType>) value, offset);
    } else if (signature.isDictionaryEntry()) {
      return new DictEntryEncoder<>(
              order,
              signature
      ).encode((DictEntry<DBusBasicType, DBusType>) value, offset);
    } else if (signature.isStruct()) {
      return new StructEncoder(
              order,
              signature
      ).encode((Struct) value, offset);
    } else if (signature.isVariant()) {
      return new VariantEncoder(order).encode((Variant) value, offset);
    } else {
      throw new EncoderException("Unsupported container type for signature: " + signature);
    }
  }
}
