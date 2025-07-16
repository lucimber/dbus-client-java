/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.decoder;

import com.lucimber.dbus.type.DBusBasicType;
import com.lucimber.dbus.type.DBusContainerType;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeCode;
import com.lucimber.dbus.type.TypeUtils;
import com.lucimber.dbus.util.LoggerUtils;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods used by the ByteBuffer-based implementations of the decoders.
 */
public final class DecoderUtils {

  static final int MAX_ARRAY_LENGTH = 67108864;
  private static final Logger LOGGER = LoggerFactory.getLogger(DecoderUtils.class);
  private static final DecoderFactory DECODER_FACTORY = new DefaultDecoderFactory();

  private DecoderUtils() {
    // Utility class
  }

  /**
   * Skips alignment padding bytes in the buffer for the given type.
   *
   * @param buffer the ByteBuffer to advance
   * @param offset the current byte offset
   * @param type   the D-Bus type requiring alignment
   * @return the number of padding bytes skipped
   */
  public static int skipPadding(ByteBuffer buffer, int offset, Type type) {
    int padding = calculateAlignmentPadding(type, offset);
    if (padding > 0) {
      buffer.position(buffer.position() + padding);
    }
    return padding;
  }

  /**
   * Calculates the number of padding bytes needed for proper alignment.
   *
   * @param type      the D-Bus type requiring alignment
   * @param byteCount the current byte count/offset
   * @return the number of padding bytes needed
   */
  public static int calculateAlignmentPadding(Type type, int byteCount) {
    int alignment = type.getAlignment().getAlignment();
    int remainder = byteCount % alignment;
    if (remainder > 0) {
      LOGGER.debug(LoggerUtils.MARSHALLING,
              "Calculating alignment padding: alignment={}; remainder={}",
              alignment, remainder);
      return alignment - remainder;
    } else {
      return 0;
    }
  }

  /**
   * Verifies that an array length is within D-Bus limits.
   *
   * @param length the array length to verify
   * @throws DecoderException if the length exceeds maximum allowed
   */
  public static void verifyArrayLength(DBusUInt32 length) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Verifying length of D-Bus array.");

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
  public static <R extends DBusType> DecoderResult<R> decode(DBusSignature signature,
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

  /**
   * Decodes a D-Bus container type from the buffer.
   *
   * @param signature signature of the container type
   * @param buffer    buffer containing the data
   * @param offset    offset inside the buffer
   * @param <R>       expected container type
   * @return decoded container type
   * @throws DecoderException if decoding fails
   */
  @SuppressWarnings("unchecked")
  public static <R extends DBusContainerType> DecoderResult<R> decodeContainerType(DBusSignature signature,
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

  /**
   * Decodes a D-Bus basic type from the buffer.
   *
   * @param code   the type code of the basic type
   * @param buffer buffer containing the data
   * @param offset offset inside the buffer
   * @param <R>    expected basic type
   * @return decoded basic type
   * @throws DecoderException if decoding fails
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <R extends DBusBasicType> DecoderResult<R> decodeBasicType(TypeCode code,
                                                                           ByteBuffer buffer,
                                                                           int offset)
          throws DecoderException {
    Decoder<ByteBuffer, DBusType> decoder = DECODER_FACTORY.createDecoder(code);
    DecoderResult result = decoder.decode(buffer, offset);
    return (DecoderResult<R>) result;
  }
}
