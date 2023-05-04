/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.decoder;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusContainerType;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeCode;
import com.lucimber.dbus.type.TypeUtils;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various methods used by the netty based implementations of the decoders.
 */
public final class DecoderUtils {

  static final int MAX_ARRAY_LENGTH = 67108864;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private DecoderUtils() {
    // Utility class
  }

  static int skipPadding(final ByteBuf buffer, final int offset, final Type type) {
    final int padding = DecoderUtils.calculateAlignmentPadding(type, offset);
    if (padding > 0) {
      buffer.skipBytes(padding);
    }
    return padding;
  }

  static int calculateAlignmentPadding(final Type type, final int byteCount) {
    final int alignment = type.getAlignment().getAlignment();
    final int remainder = byteCount % alignment;
    if (remainder > 0) {
      LoggerUtils.debug(LOGGER, () -> {
        final String s = "Calculating alignment padding: alignment=%d; remainder=%d";
        return String.format(s, alignment, remainder);
      });
      return alignment - remainder;
    } else {
      return 0;
    }
  }

  static void verifyArrayLength(final UInt32 length) {
    LoggerUtils.trace(LOGGER, () -> "Verifying length of D-Bus array.");
    Objects.requireNonNull(length, "length must not be null");
    if (Integer.compareUnsigned(length.getDelegate(), MAX_ARRAY_LENGTH) > 0) {
      final String msg = String.format("Array length (%s) exceeds maximum length (%s)",
              length, Integer.toUnsignedString(MAX_ARRAY_LENGTH));
      throw new DecoderException(msg);
    }
  }

  /**
   * Decodes D-Bus data types from a buffer.
   *
   * @param signature signature of the expected data type
   * @param buffer    buffer containing the expected data type
   * @param offset    offset inside the buffer
   * @param order     byte order used by the buffer
   * @param <R>       expected data type
   * @return decoded data type
   * @throws DecoderException If the expected data type could not be decoded from the buffer.
   */
  @SuppressWarnings("unchecked")
  public static <R extends DBusType> DecoderResult<R> decode(final Signature signature, final ByteBuf buffer,
                                                             final int offset, final ByteOrder order)
          throws DecoderException {
    Objects.requireNonNull(signature, "signature must not be null");
    Objects.requireNonNull(buffer, "buffer must not be null");
    Objects.requireNonNull(order, "order must not be null");
    if (signature.isContainerType()) {
      return (DecoderResult<R>) decodeContainerType(signature, buffer, offset, order);
    } else {
      final char c = signature.toString().charAt(0);
      final TypeCode code = TypeUtils.getCodeFromChar(c)
              .orElseThrow(() -> new DecoderException("can not map char to code: " + c));
      return (DecoderResult<R>) decodeBasicType(code, buffer, offset, order);
    }
  }

  @SuppressWarnings("unchecked")
  static <R extends DBusContainerType> DecoderResult<R> decodeContainerType(final Signature signature,
                                                                            final ByteBuf buffer, final int offset,
                                                                            final ByteOrder order)
          throws DecoderException {
    if (signature.isArray()) {
      return (DecoderResult<R>) new ArrayDecoder<>(order, signature).decode(buffer, offset);
    } else if (signature.isDictionary()) {
      return (DecoderResult<R>) new DictDecoder<>(order, signature).decode(buffer, offset);
    } else if (signature.isDictionaryEntry()) {
      return (DecoderResult<R>) new DictEntryDecoder<>(order, signature).decode(buffer, offset);
    } else if (signature.isStruct()) {
      return (DecoderResult<R>) new StructDecoder(order, signature).decode(buffer, offset);
    } else if (signature.isVariant()) {
      return (DecoderResult<R>) new VariantDecoder(order).decode(buffer, offset);
    } else {
      throw new DecoderException("Unsupported container type");
    }
  }

  @SuppressWarnings("unchecked")
  static <R extends DBusBasicType> DecoderResult<R> decodeBasicType(final TypeCode code, final ByteBuf buffer,
                                                                    final int offset, final ByteOrder order)
          throws DecoderException {
    switch (code) {
      default:
        throw new DecoderException("Unsupported basic type: " + code);
      case BOOLEAN:
        return (DecoderResult<R>) new BooleanDecoder(order).decode(buffer, offset);
      case BYTE:
        return (DecoderResult<R>) new ByteDecoder().decode(buffer, offset);
      case DOUBLE:
        return (DecoderResult<R>) new DoubleDecoder(order).decode(buffer, offset);
      case INT16:
        return (DecoderResult<R>) new Int16Decoder(order).decode(buffer, offset);
      case INT32:
        return (DecoderResult<R>) new Int32Decoder(order).decode(buffer, offset);
      case INT64:
        return (DecoderResult<R>) new Int64Decoder(order).decode(buffer, offset);
      case OBJECT_PATH:
        return (DecoderResult<R>) new ObjectPathDecoder(order).decode(buffer, offset);
      case SIGNATURE:
        return (DecoderResult<R>) new SignatureDecoder().decode(buffer, offset);
      case STRING:
        return (DecoderResult<R>) new StringDecoder(order).decode(buffer, offset);
      case UINT16:
        return (DecoderResult<R>) new UInt16Decoder(order).decode(buffer, offset);
      case UINT32:
        return (DecoderResult<R>) new UInt32Decoder(order).decode(buffer, offset);
      case UINT64:
        return (DecoderResult<R>) new UInt64Decoder(order).decode(buffer, offset);
      case UNIX_FD:
        return (DecoderResult<R>) new UnixFdDecoder(order).decode(buffer, offset);
    }
  }
}
