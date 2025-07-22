/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.util;

import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusVariant;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting typed values from D-Bus message header fields.
 * This class provides a generic way to extract and validate header field values,
 * eliminating code duplication in message decoders.
 */
public final class HeaderFieldExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeaderFieldExtractor.class);

  private HeaderFieldExtractor() {
    // Utility class
  }

  /**
   * Extracts an optional header field value of the specified type.
   *
   * @param <T> the expected D-Bus type
   * @param headerFields the map of header fields
   * @param field the header field to extract
   * @param expectedType the expected type class
   * @return an Optional containing the value if present and of correct type
   * @throws CorruptedFrameException if the field is present but of wrong type
   */
  public static <T extends DBusType> Optional<T> extractOptional(
      Map<HeaderField, DBusVariant> headerFields,
      HeaderField field,
      Class<T> expectedType) {
    
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting {} from message header", field.name());
    
    DBusVariant variant = headerFields.get(field);
    if (variant == null) {
      return Optional.empty();
    }
    
    DBusType variantValue = variant.getDelegate();
    if (expectedType.isInstance(variantValue)) {
      return Optional.of(expectedType.cast(variantValue));
    } else {
      String msg = String.format("%s in message header is of wrong type. Expected %s but got %s",
          field.name(), expectedType.getSimpleName(), variantValue.getClass().getSimpleName());
      throw new CorruptedFrameException(msg);
    }
  }

  /**
   * Extracts a required header field value of the specified type.
   *
   * @param <T> the expected D-Bus type
   * @param headerFields the map of header fields
   * @param field the header field to extract
   * @param expectedType the expected type class
   * @return the value if present and of correct type
   * @throws CorruptedFrameException if the field is missing or of wrong type
   */
  public static <T extends DBusType> T extractRequired(
      Map<HeaderField, DBusVariant> headerFields,
      HeaderField field,
      Class<T> expectedType) {
    
    return extractOptional(headerFields, field, expectedType)
        .orElseThrow(() -> new CorruptedFrameException(
            String.format("Missing %s in message header", field.name())));
  }

  /**
   * Extracts an optional string header field value.
   *
   * @param headerFields the map of header fields
   * @param field the header field to extract
   * @return an Optional containing the string value if present
   * @throws CorruptedFrameException if the field is present but not a string
   */
  public static Optional<String> extractOptionalString(
      Map<HeaderField, DBusVariant> headerFields,
      HeaderField field) {
    
    return extractOptional(headerFields, field, com.lucimber.dbus.type.DBusString.class)
        .map(com.lucimber.dbus.type.DBusString::getDelegate);
  }

  /**
   * Extracts a required string header field value.
   *
   * @param headerFields the map of header fields
   * @param field the header field to extract
   * @return the string value
   * @throws CorruptedFrameException if the field is missing or not a string
   */
  public static String extractRequiredString(
      Map<HeaderField, DBusVariant> headerFields,
      HeaderField field) {
    
    return extractRequired(headerFields, field, com.lucimber.dbus.type.DBusString.class)
        .getDelegate();
  }
}