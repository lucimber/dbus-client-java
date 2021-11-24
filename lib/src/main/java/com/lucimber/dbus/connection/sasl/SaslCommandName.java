package com.lucimber.dbus.connection.sasl;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all SASL command names used by D-Bus.
 */
public enum SaslCommandName {
  SHARED_DATA("DATA"),
  SHARED_ERROR("ERROR"),
  CLIENT_AUTH("AUTH"),
  CLIENT_BEGIN("BEGIN"),
  CLIENT_CANCEL("CANCEL"),
  CLIENT_NEGOTIATE("NEGOTIATE_UNIX_FD"),
  SERVER_AGREE("AGREE_UNIX_FD"),
  SERVER_OK("OK"),
  SERVER_REJECTED("REJECTED");

  private static final Map<String, SaslCommandName> STRING_TO_ENUM = new HashMap<>();

  static {
    for (SaslCommandName commandName : values()) {
      STRING_TO_ENUM.put(commandName.toString(), commandName);
    }
  }

  private final String customName;

  SaslCommandName(final String customName) {
    this.customName = customName;
  }

  /**
   * Translates the custom string representation back to the corresponding enum.
   *
   * @param customName The custom string representation.
   * @return The corresponding enum.
   */
  public static SaslCommandName fromString(final String customName) {
    return STRING_TO_ENUM.get(customName);
  }

  @Override
  public String toString() {
    return customName;
  }
}
