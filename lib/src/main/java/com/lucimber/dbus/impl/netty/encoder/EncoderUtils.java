/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.encoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.protocol.types.DBusArray;
import com.lucimber.dbus.protocol.types.DBusBasicType;
import com.lucimber.dbus.protocol.types.DBusBoolean;
import com.lucimber.dbus.protocol.types.DBusByte;
import com.lucimber.dbus.protocol.types.DBusContainerType;
import com.lucimber.dbus.protocol.types.DBusDouble;
import com.lucimber.dbus.protocol.types.DBusString;
import com.lucimber.dbus.protocol.types.DBusType;
import com.lucimber.dbus.protocol.types.Dict;
import com.lucimber.dbus.protocol.types.DictEntry;
import com.lucimber.dbus.protocol.types.Int16;
import com.lucimber.dbus.protocol.types.Int32;
import com.lucimber.dbus.protocol.types.Int64;
import com.lucimber.dbus.protocol.types.ObjectPath;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.protocol.types.Struct;
import com.lucimber.dbus.protocol.types.Type;
import com.lucimber.dbus.protocol.types.TypeAlignment;
import com.lucimber.dbus.protocol.types.UInt16;
import com.lucimber.dbus.protocol.types.UInt32;
import com.lucimber.dbus.protocol.types.UInt64;
import com.lucimber.dbus.protocol.types.UnixFd;
import com.lucimber.dbus.protocol.types.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Various methods used by the netty based implementations of the encoders.
 */
public final class EncoderUtils {

  private EncoderUtils() {
    // Utility class
  }

  static int applyPadding(final ByteBuf buffer, final int offset, final Type type) {
    final int padding = calculateAlignmentPadding(type.getAlignment(), offset);
    if (padding > 0) {
      buffer.writeZero(padding);
    }
    return padding;
  }

  static int calculateAlignmentPadding(final TypeAlignment alignment, final int offset) {
    final int remainder = offset % alignment.getAlignment();
    if (remainder > 0) {
      return alignment.getAlignment() - remainder;
    } else {
      return 0;
    }
  }

  /**
   * Encodes a data type into its binary representation and returns that representation in a buffer.
   *
   * @param value  the data type
   * @param offset the offset inside the buffer
   * @param order  the byte order used by the buffer
   * @return the encoder result with a buffer
   * @throws EncoderException If the given value could not be encoded.
   */
  public static EncoderResult<ByteBuf> encode(final DBusType value, final int offset,
                                              final ByteOrder order) throws EncoderException {
    if (value instanceof DBusContainerType) {
      return encodeContainerType((DBusContainerType) value, offset, order);
    } else {
      return encodeBasicType((DBusBasicType) value, offset, order);
    }
  }

  static EncoderResult<ByteBuf> encodeBasicType(final DBusBasicType value, final int offset,
                                                final ByteOrder order) throws EncoderException {
    final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
    if (value instanceof DBusBoolean) {
      return new BooleanEncoder(allocator, order).encode((DBusBoolean) value, offset);
    } else if (value instanceof DBusByte) {
      return new ByteEncoder(allocator).encode((DBusByte) value, offset);
    } else if (value instanceof DBusDouble) {
      return new DoubleEncoder(allocator, order).encode((DBusDouble) value, offset);
    } else if (value instanceof Int32) {
      return new Int32Encoder(allocator, order).encode((Int32) value, offset);
    } else if (value instanceof Int64) {
      return new Int64Encoder(allocator, order).encode((Int64) value, offset);
    } else if (value instanceof ObjectPath) {
      return new ObjectPathEncoder(allocator, order).encode((ObjectPath) value, offset);
    } else if (value instanceof Int16) {
      return new Int16Encoder(allocator, order).encode((Int16) value, offset);
    } else if (value instanceof Signature) {
      return new SignatureEncoder(allocator).encode((Signature) value, offset);
    } else if (value instanceof DBusString) {
      return new StringEncoder(allocator, order).encode((DBusString) value, offset);
    } else if (value instanceof UnixFd) {
      return new UnixFdEncoder(allocator, order).encode((UnixFd) value, offset);
    } else if (value instanceof UInt32) {
      return new UInt32Encoder(allocator, order).encode((UInt32) value, offset);
    } else if (value instanceof UInt64) {
      return new UInt64Encoder(allocator, order).encode((UInt64) value, offset);
    } else if (value instanceof UInt16) {
      return new UInt16Encoder(allocator, order).encode((UInt16) value, offset);
    } else {
      throw new EncoderException("Unknown D-Bus data type: " + value);
    }
  }

  static EncoderResult<ByteBuf> encodeContainerType(final DBusContainerType value, final int offset,
                                                    final ByteOrder order) throws EncoderException {
    final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
    try {
      final Signature signature = value.getSignature();
      if (signature.isArray()) {
        @SuppressWarnings("unchecked") final DBusArray<DBusType> array = (DBusArray<DBusType>) value;
        return new ArrayEncoder<>(allocator, order, signature).encode(array, offset);
      } else if (signature.isDictionary()) {
        @SuppressWarnings("unchecked") final Dict<DBusBasicType, DBusType> dict = (Dict<DBusBasicType, DBusType>) value;
        return new DictEncoder<>(allocator, order, signature).encode(dict, offset);
      } else if (signature.isDictionaryEntry()) {
        @SuppressWarnings("unchecked") final DictEntry<DBusBasicType, DBusType> dictEntry =
                (DictEntry<DBusBasicType, DBusType>) value;
        return new DictEntryEncoder<>(allocator, order, signature).encode(dictEntry, offset);
      } else if (signature.isStruct()) {
        final Struct struct = (Struct) value;
        return new StructEncoder(allocator, order, signature).encode(struct, offset);
      } else if (signature.isVariant()) {
        final Variant variant = (Variant) value;
        return new VariantEncoder(allocator, order).encode(variant, offset);
      }
    } catch (ClassCastException e) {
      throw new EncoderException("Mismatch between signature and value.", e);
    }
    throw new EncoderException("Unknown container type");
  }
}
