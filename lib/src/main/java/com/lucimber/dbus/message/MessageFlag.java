package com.lucimber.dbus.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains all message flags used by D-Bus.
 */
public enum MessageFlag {

  /**
   * This message does not expect method return replies or error replies,
   * even if it is of a type that can have a reply; the reply should be omitted.
   *
   * <p>Note that METHOD_CALL is the only message type currently defined
   * in this specification that can expect a reply, so the presence or absence
   * of this flag in the other three message types that are currently documented
   * is meaningless: replies to those message types should not be sent,
   * whether this flag is present or not.
   */
  NO_REPLY_EXPECTED("NO_REPLY_EXPECTED"),

  /**
   * The bus must not launch an owner for the destination name in response to this message.
   */
  NO_AUTO_START("NO_AUTO_START"),

  /**
   * This flag may be set on a method call message to inform the receiving side
   * that the caller is prepared to wait for interactive authorization, which
   * might take a considerable time to complete. For instance, if this flag is set,
   * it would be appropriate to query the user for passwords or confirmation
   * via Polkit or a similar framework.
   *
   * <p>This flag is only useful when unprivileged code calls a more privileged method call,
   * and an authorization framework is deployed that allows possibly interactive authorization.
   * If no such framework is deployed it has no effect. This flag should not be set by default
   * by client implementations. If it is set, the caller should also set a suitably long timeout
   * on the method call to make sure the user interaction may complete. This flag is only valid
   * for method call messages, and shall be ignored otherwise.
   *
   * <p>Interaction that takes place as a part of the effect of the method being called
   * is outside the scope of this flag, even if it could also be characterized as authentication
   * or authorization. For instance, in a method call that directs a network management service
   * to attempt to connect to a virtual private network, this flag should control how the network
   * management service makes the decision "is this user allowed to change system network configuration?",
   * but it should not affect how or whether the network management service interacts with the user
   * to obtain the credentials that are required for access to the VPN.
   *
   * <p>If a this flag is not set on a method call, and a service determines that the requested operation
   * is not allowed without interactive authorization, but could be allowed after successful
   * interactive authorization, it may return the org.freedesktop.DBus.Error.InteractiveAuthorizationRequired error.
   *
   * <p>The absence of this flag does not guarantee that interactive authorization will not be applied,
   * since existing services that pre-date this flag might already use interactive authorization.
   * However, existing D-Bus APIs that will use interactive authorization should document that
   * the call may take longer than usual, and new D-Bus APIs should avoid interactive authorization
   * in the absence of this flag.
   */
  ALLOW_INTERACTIVE_AUTHORIZATION("ALLOW_INTERACTIVE_AUTHORIZATION");

  private static final Map<String, MessageFlag> STRING_TO_ENUM = new HashMap<>();

  static {
    for (MessageFlag flag : values()) {
      STRING_TO_ENUM.put(flag.toString(), flag);
    }
  }

  private final String customName;

  MessageFlag(final String customName) {
    this.customName = customName;
  }

  /**
   * Translates the custom string representation back to the corresponding enum.
   *
   * @param customName The custom string representation.
   * @return a {@link MessageFlag}.
   */
  public static MessageFlag fromString(final String customName) {
    return STRING_TO_ENUM.get(customName);
  }

  @Override
  public String toString() {
    return customName;
  }
}
