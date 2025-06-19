/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.*;
import com.lucimber.dbus.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Utility methods used by the ByteBuffer-based implementations of the decoders.
 */
public final class DecoderUtils {

  static final int MAX_ARRAY_LENGTH = 67108864;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private DecoderUtils() {
    // Utility class
  }

  public static int skipPadding(ByteBuffer buffer, int offset, Type type) {
    int padding = calculateAlignmentPadding(type, offset);
    if (padding > 0) {
      buffer.position(buffer.position() + padding);
    }
    return padding;
  }

  public static int calculateAlignmentPadding(Type type, int byteCount) {
    int alignment = type.getAlignment().getAlignment();
    int remainder = byteCount % alignment;
    if (remainder > 0) {
      LoggerUtils.debug(LOGGER, () -> {
        String s = "Calculating alignment padding: alignment=%d; remainder=%d";
        return String.format(s, alignment, remainder);
      });
      return alignment - remainder;
    } else {
      return 0;
    }
  }

  public static void verifyArrayLength(UInt32 length) {
    LoggerUtils.trace(LOGGER, () -> "Verifying length of D-Bus array.");
    Objects.requireNonNull(length, "length must not be null");
    if (Integer.compareUnsigned(length.getDelegate(), MAX_ARRAY_LENGTH) > 0) {
      String msg = String.format("Array length (%s) exceeds maximum length (%s)",
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
   * @param <R>       expected data type
   * @return decoded data type
   * @throws DecoderException If the expected data type could not be decoded from the buffer.
   */
  @SuppressWarnings("unchecked")
  public static <R extends DBusType> DecoderResult<R> decode(Signature signature,
                                                             ByteBuffer buffer,
                                                             int offset)
        throws DecoderException {
    Objects.requireNonNull(signature, "signature must not be null");
    Objects.requireNonNull(buffer, "buffer must not be null");
    if (signature.isContainerType()) {
      return (DecoderResult<R>) decodeContainerType(signature, buffer, offset);
    } else {
      char c = signature.toString().charAt(0);
      TypeCode code = TypeUtils.getCodeFromChar(c)
            .orElseThrow(() -> new DecoderException("Cannot map char to code: " + c));
      return (DecoderResult<R>) decodeBasicType(code, buffer, offset);
    }
  }

  @SuppressWarnings("unchecked")
  public static <R extends DBusContainerType> DecoderResult<R> decodeContainerType(Signature signature,
                                                                                   ByteBuffer buffer,
                                                                                   int offset)
        throws DecoderException {
    if (signature.isArray()) {
      return (DecoderResult<R>) new ArrayDecoder<>(signature).decode(buffer, offset);
    } else if (signature.isDictionary()) {
      return (DecoderResult<R>) new DictDecoder<>(signature).decode(buffer, offset);
    } else if (signature.isDictionaryEntry()) {
      return (DecoderResult<R>) new DictEntryDecoder<>(signature).decode(buffer, offset);
    } else if (signature.isStruct()) {
      return (DecoderResult<R>) new StructDecoder(signature).decode(buffer, offset);
    } else if (signature.isVariant()) {
      return (DecoderResult<R>) new VariantDecoder().decode(buffer, offset);
    } else {
      throw new DecoderException("Unsupported container type");
    }
  }

  @SuppressWarnings("unchecked")
  public static <R extends DBusBasicType> DecoderResult<R> decodeBasicType(TypeCode code,
                                                                           ByteBuffer buffer,
                                                                           int offset)
        throws DecoderException {
    return switch (code) {
      case BOOLEAN -> (DecoderResult<R>) new BooleanDecoder().decode(buffer, offset);
      case BYTE -> (DecoderResult<R>) new ByteDecoder().decode(buffer, offset);
      case DOUBLE -> (DecoderResult<R>) new DoubleDecoder().decode(buffer, offset);
      case INT16 -> (DecoderResult<R>) new Int16Decoder().decode(buffer, offset);
      case INT32 -> (DecoderResult<R>) new Int32Decoder().decode(buffer, offset);
      case INT64 -> (DecoderResult<R>) new Int64Decoder().decode(buffer, offset);
      case OBJECT_PATH -> (DecoderResult<R>) new ObjectPathDecoder().decode(buffer, offset);
      case SIGNATURE -> (DecoderResult<R>) new SignatureDecoder().decode(buffer, offset);
      case STRING -> (DecoderResult<R>) new StringDecoder().decode(buffer, offset);
      case UINT16 -> (DecoderResult<R>) new UInt16Decoder().decode(buffer, offset);
      case UINT32 -> (DecoderResult<R>) new UInt32Decoder().decode(buffer, offset);
      case UINT64 -> (DecoderResult<R>) new UInt64Decoder().decode(buffer, offset);
      case UNIX_FD -> (DecoderResult<R>) new UnixFdDecoder().decode(buffer, offset);
      default -> throw new DecoderException("Unsupported basic type: " + code);
    };
  }
}
