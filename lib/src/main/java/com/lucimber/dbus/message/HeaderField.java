package com.lucimber.dbus.message;

import com.lucimber.dbus.type.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains all header fields used by D-Bus.
 */
public enum HeaderField {

  /**
   * The name of the connection this message is intended for.
   * This field is usually only meaningful in combination
   * with the message bus (see the section called “Message Bus Specification”),
   * but other servers may define their own meanings for it.
   * This header field is controlled by the message sender.
   */
  DESTINATION("DESTINATION", 6, Type.STRING),

  /**
   * The name of the error that occurred; used for errors.
   */
  ERROR_NAME("ERROR_NAME", 4, Type.STRING),

  /**
   * The interface to invoke a method call on, or that a signal is emitted from.
   * Optional for method calls, required for signals.
   * The special interface {@code org.freedesktop.DBus.Local} is reserved;
   * implementations should not send messages with this interface,
   * and the reference implementation of the bus daemon will disconnect
   * any application that attempts to do so.
   * This header field is controlled by the message sender.
   */
  INTERFACE("INTERFACE", 2, Type.STRING),

  /**
   * The member, either the method name or signal name.
   * This header field is controlled by the message sender.
   */
  MEMBER("MEMBER", 3, Type.STRING),

  /**
   * The serial number of the message this message is a reply to.
   * (The serial number is the second UINT32 in the header.)
   * This header field is controlled by the message sender.
   */
  REPLY_SERIAL("REPLY_SERIAL", 5, Type.UINT32),

  /**
   * Unique name of the sending connection.
   * This field is usually only meaningful in combination with the message bus,
   * but other servers may define their own meanings for it.
   * On a message bus, this header field is controlled by the message bus,
   * so it is as reliable and trustworthy as the message bus itself.
   * Otherwise, this header field is controlled by the message sender,
   * unless there is out-of-band information that indicates otherwise.
   */
  SENDER("SENDER", 7, Type.STRING),

  /**
   * The signature of the message body.
   * If omitted, it is assumed to be the empty signature "" (i.e. the body must be 0-length).
   * This header field is controlled by the message sender.
   */
  SIGNATURE("SIGNATURE", 8, Type.SIGNATURE),

  /**
   * The object to send a call to, or the object a signal is emitted from.
   * The special path {@code /org/freedesktop/DBus/Local} is reserved;
   * implementations should not send messages with this path,
   * and the reference implementation of the bus daemon will disconnect
   * any application that attempts to do so.
   * This header field is controlled by the message sender.
   */
  PATH("PATH", 1, Type.OBJECT_PATH),

  /**
   * The number of Unix file descriptors that accompany the message.
   * If omitted, it is assumed that no Unix file descriptors accompany the message.
   * The actual file descriptors need to be transferred via platform specific mechanism out-of-band.
   * They must be sent at the same time as part of the message itself.
   * They may not be sent before the first byte of the message itself is transferred
   * or after the last byte of the message itself.
   * This header field is controlled by the message sender.
   */
  UNIX_FDS("UNIX_FDS", 9, Type.UINT32);

  private static final Map<Integer, HeaderField> INTEGER_TO_ENUM = new HashMap<>();
  private static final Map<String, HeaderField> STRING_TO_ENUM = new HashMap<>();

  static {
    for (HeaderField headerField : values()) {
      STRING_TO_ENUM.put(headerField.toString(), headerField);
      INTEGER_TO_ENUM.put(headerField.getDecimalCode(), headerField);
    }
  }

  private final String customName;
  private final int decimalCode;
  private final Type type;

  HeaderField(final String customName, final int decimalCode, final Type type) {
    this.customName = customName;
    this.decimalCode = decimalCode;
    this.type = type;
  }

  /**
   * Translates the custom string representation back to the corresponding enum.
   *
   * @param customName The custom string representation.
   * @return The corresponding {@link HeaderField}.
   */
  public static HeaderField fromString(final String customName) {
    return STRING_TO_ENUM.get(customName);
  }

  public static HeaderField fromDecimalCode(final int decimalCode) {
    return INTEGER_TO_ENUM.get(decimalCode);
  }

  @Override
  public String toString() {
    return customName;
  }

  /**
   * Gets the decimal code of the header field.
   *
   * @return The decimal code as an {@link Integer}.
   */
  public int getDecimalCode() {
    return decimalCode;
  }

  /**
   * Gets the associated {@link Type}.
   *
   * @return The D-Bus type.
   */
  public Type getType() {
    return type;
  }
}
