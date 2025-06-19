/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.decoder.DecoderException;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageFlag;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.type.DBusByte;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Struct;
import com.lucimber.dbus.type.Variant;
import io.netty.buffer.ByteBuf;

import java.nio.ByteOrder;
import java.util.*;

/**
 * Utility class for common methods used for decoding and encoding messages.
 */
final class InboundUtils {

  private static final int MAX_MSG_LENGTH = 0x08000000;
  private static final int ZERO = 0x00000000;

  private InboundUtils() {
    // Utility class
  }

  static ByteOrder decodeByteOrder(final ByteBuf buffer) throws DecoderException {
    Objects.requireNonNull(buffer, "buffer must not be null");
    final byte B = 0x42;
    final byte l = 0x6C;
    final byte byteOrder = buffer.readByte();
    if (byteOrder == B) {
      return ByteOrder.BIG_ENDIAN;
    } else if (byteOrder == l) {
      return ByteOrder.LITTLE_ENDIAN;
    } else {
      throw new DecoderException("unknown byte order");
    }
  }

  static MessageType decodeType(ByteBuf buffer) {
    Objects.requireNonNull(buffer, "buffer must not be null");
    byte ub = buffer.readByte();
    return MessageType.fromCode(ub);
  }

  static Set<MessageFlag> decodeFlags(final ByteBuf buffer) {
    Objects.requireNonNull(buffer, "buffer must not be null");
    final byte flagsByte = buffer.readByte();
    final Set<MessageFlag> flags = new HashSet<>();
    final byte replyFlag = 0x01;
    final byte startFlag = 0x02;
    final byte authFlag = 0x04;
    if ((flagsByte & replyFlag) == replyFlag) {
      flags.add(MessageFlag.NO_REPLY_EXPECTED);
    } else if ((flagsByte & startFlag) == startFlag) {
      flags.add(MessageFlag.NO_AUTO_START);
    } else if ((flagsByte & authFlag) == authFlag) {
      flags.add(MessageFlag.ALLOW_INTERACTIVE_AUTHORIZATION);
    }
    return flags;
  }

  static Map<HeaderField, Variant> mapHeaderFields(List<Struct> headerFields) {
    Map<HeaderField, Variant> map = new HashMap<>();
    for (Struct struct : headerFields) {
      List<DBusType> structList = struct.getDelegate();
      DBusByte dbusByte = (DBusByte) structList.get(0);
      HeaderField headerField = HeaderField.fromCode(dbusByte.getDelegate());
      Variant variant = (Variant) structList.get(1);
      map.put(headerField, variant);
    }
    return map;
  }

  static boolean isMessageTooLong(final int headerLength, final int bodyLength) {
    final int signature = 0x0C;
    final int headerSignature = 0x08;
    final int headerAlignment = 0x08;
    final int headerRemainder = Integer.remainderUnsigned(headerLength, headerAlignment);
    final int headerPadding = headerAlignment - headerRemainder;
    int messageLength = signature + headerSignature + headerLength;
    if (Integer.compareUnsigned(headerRemainder, ZERO) > 0) {
      messageLength += headerPadding;
    }
    messageLength += bodyLength;
    return Integer.compareUnsigned(messageLength, MAX_MSG_LENGTH) > 0;
  }
}
