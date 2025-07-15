/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.decoder.DecoderUtils;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.Message;
import com.lucimber.dbus.message.MessageFlag;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import com.lucimber.dbus.type.DBusVariant;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An inbound handler that decodes message frames into typed messages.
 *
 * @see Frame
 * @see Message
 */
final class InboundMessageDecoder extends MessageToMessageDecoder<Frame> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  private static Optional<DBusString> getErrorNameFromHeader(Map<HeaderField, DBusVariant> headerFields) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting signature from message header.");
    DBusVariant variant = headerFields.get(HeaderField.ERROR_NAME);
    if (variant == null) {
      return Optional.empty();
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return Optional.of((DBusString) variantValue);
    } else {
      String msg = "Error name in message header is of wrong type.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static Optional<DBusString> getInterfaceNameFromHeader(Map<HeaderField, DBusVariant> headerFields) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting interface name from message header.");
    DBusVariant variant = headerFields.get(HeaderField.INTERFACE);
    if (variant == null) {
      return Optional.empty();
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return Optional.of((DBusString) variantValue);
    } else {
      String msg = "Interface name in message header is of wrong type.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static DBusString getMemberFromHeader(Map<HeaderField, DBusVariant> headerFields) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting member from message header.");
    DBusVariant variant = headerFields.get(HeaderField.MEMBER);
    if (variant == null) {
      String msg = "Missing member in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return (DBusString) variantValue;
    } else {
      String msg = "Member in message header is of wrong type.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static DBusObjectPath getObjectPathFromHeader(Map<HeaderField, DBusVariant> headerFields) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting object path from message header.");
    DBusVariant variant = headerFields.get(HeaderField.PATH);
    if (variant == null) {
      String msg = "Missing object path in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusObjectPath) {
      return (DBusObjectPath) variantValue;
    } else {
      String msg = "Object path in message header must be an ObjectPath.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static DBusUInt32 getReplySerialFromHeader(Map<HeaderField, DBusVariant> headerFields) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting reply serial from message header.");
    DBusVariant variant = headerFields.get(HeaderField.REPLY_SERIAL);
    if (variant == null) {
      String msg = "Missing reply serial in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusUInt32) {
      return (DBusUInt32) variantValue;
    } else {
      String msg = "Reply serial in message header must be an UINT32.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static Optional<DBusSignature> getSignatureFromHeader(Map<HeaderField, DBusVariant> headerFields) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting signature from message header.");
    DBusVariant variant = headerFields.get(HeaderField.SIGNATURE);
    if (variant == null) {
      return Optional.empty();
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusSignature) {
      return Optional.of((DBusSignature) variantValue);
    } else {
      String msg = "Signature in message header be a signature.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static DBusString getSenderFromHeader(Map<HeaderField, DBusVariant> headerFields) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Getting sender from message header.");
    DBusVariant variant = headerFields.get(HeaderField.SENDER);
    if (variant == null) {
      String msg = "Missing sender in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return (DBusString) variantValue;
    } else {
      final String msg = "Sender in message header must be a string.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static InboundError mapToError(Frame frame) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Mapping frame to inbound error.");
    DBusUInt32 serial = frame.getSerial();
    Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
    DBusUInt32 replySerial = getReplySerialFromHeader(headerFields);
    DBusString sender = getSenderFromHeader(headerFields);
    Optional<DBusString> errorName = getErrorNameFromHeader(headerFields);
    if (errorName.isEmpty()) {
      String msg = "Missing error name in message header.";
      throw new CorruptedFrameException(msg);
    }
    Optional<DBusSignature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundError(serial, replySerial, sender, errorName.get(),
            sig.orElse(null), payload);
  }

  private static InboundMethodCall mapToMethodCall(Frame frame) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Mapping frame to inbound method call.");
    DBusUInt32 serial = frame.getSerial();
    Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
    DBusString sender = getSenderFromHeader(headerFields);
    DBusObjectPath path = getObjectPathFromHeader(headerFields);
    DBusString member = getMemberFromHeader(headerFields);
    boolean replyExpected = !frame.getFlags().contains(MessageFlag.NO_REPLY_EXPECTED);
    Optional<DBusString> iface = getInterfaceNameFromHeader(headerFields);
    Optional<DBusSignature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundMethodCall(serial, sender, path, member, replyExpected,
            iface.orElse(null), sig.orElse(null), payload);
  }

  private static InboundMethodReturn mapToMethodReturn(Frame frame) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Mapping frame to inbound method return.");
    DBusUInt32 serial = frame.getSerial();
    Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
    DBusUInt32 replySerial = getReplySerialFromHeader(headerFields);
    DBusString sender = getSenderFromHeader(headerFields);
    Optional<DBusSignature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundMethodReturn(serial, replySerial, sender,
            sig.orElse(null), payload);
  }

  private static InboundSignal mapToSignal(Frame frame) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Mapping frame to inbound signal.");
    DBusUInt32 serial = frame.getSerial();
    Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
    DBusString sender = getSenderFromHeader(headerFields);
    DBusObjectPath path = getObjectPathFromHeader(headerFields);
    Optional<DBusString> iface = getInterfaceNameFromHeader(headerFields);
    if (iface.isEmpty()) {
      String msg = "Missing interface name in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusString member = getMemberFromHeader(headerFields);
    Optional<DBusSignature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundSignal(serial, sender, path, iface.get(), member,
            sig.orElse(null), payload);
  }

  private static List<DBusType> decodeFrameBody(ByteBuffer body, DBusSignature sig) {
    LOGGER.trace(LoggerUtils.MARSHALLING, "Decoding frame body.");
    ArrayList<DBusType> list = new ArrayList<>();
    try {
      if (sig.getQuantity() == 1) {
        int offset = 0;
        var result = DecoderUtils.decode(sig, body, offset);
        list.add(result.getValue());
      } else {
        List<DBusSignature> subSignatures = sig.getChildren();
        int offset = 0;
        for (DBusSignature s : subSignatures) {
          var result = DecoderUtils.decode(s, body, offset);
          list.add(result.getValue());
          offset += result.getConsumedBytes();
        }
      }
    } catch (Exception ex) {
      String failureMsg = "Couldn't decode frame body.";
      LOGGER.error(LoggerUtils.MARSHALLING, failureMsg, ex);
      throw new CorruptedFrameException(failureMsg, ex);
    }
    list.trimToSize();
    return list;
  }

  @Override
  public void decode(ChannelHandlerContext ctx, Frame frame, List<Object> out) {
    LOGGER.debug(LoggerUtils.MARSHALLING, "Decoding frame: type={}, serial={}",
            frame.getType(), frame.getSerial());
    InboundMessage msg = mapFrameToInboundMessage(frame);
    if (msg == null) {
      LOGGER.warn(LoggerUtils.MARSHALLING, "Failed to decode frame with invalid type: {}", frame.getType());
    } else {
      LOGGER.debug(LoggerUtils.MARSHALLING, "Decoded {} with serial={}",
              msg.getClass().getSimpleName(), msg.getSerial());
      out.add(msg);
    }
  }

  private InboundMessage mapFrameToInboundMessage(Frame frame) {
    return switch (frame.getType()) {
      case ERROR -> mapToError(frame);
      case METHOD_CALL -> mapToMethodCall(frame);
      case METHOD_RETURN -> mapToMethodReturn(frame);
      case SIGNAL -> mapToSignal(frame);
    };
  }
}
