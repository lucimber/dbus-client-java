/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains all message types used by D-Bus.
 */
public enum MessageType {

  /**
   * An invalid message type.
   */
  INVALID("INVALID", -1),

  /**
   * Method call. This message type may prompt a reply.
   *
   * @see InboundMethodCall
   * @see OutboundMethodCall
   */
  METHOD_CALL("METHOD_CALL", 1),

  /**
   * Method reply with returned data.
   *
   * @see InboundMethodReturn
   * @see OutboundMethodReturn
   */
  METHOD_RETURN("METHOD_RETURN", 2),

  /**
   * Error reply. If the first argument exists and is a string, it is an error message.
   *
   * @see InboundError
   * @see OutboundError
   */
  ERROR("ERROR", 3),

  /**
   * Signal emission.
   *
   * @see InboundSignal
   * @see OutboundSignal
   */
  SIGNAL("SIGNAL", 4);

  private static final Map<Integer, MessageType> INTEGER_TO_ENUM = new HashMap<>();
  private static final Map<String, MessageType> STRING_TO_ENUM = new HashMap<>();

  static {
    for (MessageType type : values()) {
      STRING_TO_ENUM.put(type.toString(), type);
      INTEGER_TO_ENUM.put(type.getDecimalCode(), type);
    }
  }

  private final String customName;
  private final int decimalCode;

  MessageType(final String customName, final int decimalCode) {
    this.customName = Objects.requireNonNull(customName);
    this.decimalCode = decimalCode;
  }

  /**
   * Translates the custom string representation back to the corresponding enum.
   *
   * @param customName a {@link String}
   * @return a {@link MessageType}
   */
  public static MessageType fromString(final String customName) {
    return STRING_TO_ENUM.get(customName);
  }

  /**
   * Translates the decimal code back to the corresponding enum.
   *
   * @param decimalCode an {@link Integer}
   * @return a {@link MessageType}.
   */
  public static MessageType fromDecimalCode(final int decimalCode) {
    return INTEGER_TO_ENUM.get(decimalCode);
  }

  /**
   * Gets the decimal code of this message type.
   *
   * @return an {@link Integer}.
   */
  public int getDecimalCode() {
    return decimalCode;
  }

  @Override
  public String toString() {
    return customName;
  }
}
