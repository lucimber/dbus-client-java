/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.decoder.DecoderUtils;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.*;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An inbound handler that decodes message frames into typed messages.
 *
 * @see Frame
 * @see Message
 */
final class FrameDecoder extends MessageToMessageDecoder<Frame> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_INBOUND);

  private static Optional<DBusString> getErrorNameFromHeader(Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting signature from message header.");
    Variant variant = headerFields.get(HeaderField.ERROR_NAME);
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

  private static Optional<DBusString> getInterfaceNameFromHeader(Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting interface name from message header.");
    Variant variant = headerFields.get(HeaderField.INTERFACE);
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

  private static DBusString getMemberFromHeader(Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting member from message header.");
    Variant variant = headerFields.get(HeaderField.MEMBER);
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

  private static ObjectPath getObjectPathFromHeader(Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting object path from message header.");
    Variant variant = headerFields.get(HeaderField.PATH);
    if (variant == null) {
      String msg = "Missing object path in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof ObjectPath) {
      return (ObjectPath) variantValue;
    } else {
      String msg = "Object path in message header must be an ObjectPath.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static UInt32 getReplySerialFromHeader(Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting reply serial from message header.");
    Variant variant = headerFields.get(HeaderField.REPLY_SERIAL);
    if (variant == null) {
      String msg = "Missing reply serial in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof UInt32) {
      return (UInt32) variantValue;
    } else {
      String msg = "Reply serial in message header must be an UINT32.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static Optional<Signature> getSignatureFromHeader(Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting signature from message header.");
    Variant variant = headerFields.get(HeaderField.SIGNATURE);
    if (variant == null) {
      return Optional.empty();
    }
    DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof Signature) {
      return Optional.of((Signature) variantValue);
    } else {
      String msg = "Signature in message header be a signature.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static DBusString getSenderFromHeader(Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting sender from message header.");
    Variant variant = headerFields.get(HeaderField.SENDER);
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
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound error.");
    UInt32 serial = frame.getSerial();
    Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    UInt32 replySerial = getReplySerialFromHeader(headerFields);
    DBusString sender = getSenderFromHeader(headerFields);
    Optional<DBusString> errorName = getErrorNameFromHeader(headerFields);
    if (errorName.isEmpty()) {
      String msg = "Missing error name in message header.";
      throw new CorruptedFrameException(msg);
    }
    Optional<Signature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundError(serial, replySerial, sender, errorName.get(),
          sig.orElse(null), payload);
  }

  private static InboundMethodCall mapToMethodCall(Frame frame) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound method call.");
    UInt32 serial = frame.getSerial();
    Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    DBusString sender = getSenderFromHeader(headerFields);
    ObjectPath path = getObjectPathFromHeader(headerFields);
    DBusString member = getMemberFromHeader(headerFields);
    boolean replyExpected = !frame.getFlags().contains(MessageFlag.NO_REPLY_EXPECTED);
    Optional<DBusString> iface = getInterfaceNameFromHeader(headerFields);
    Optional<Signature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundMethodCall(serial, sender, path, member, replyExpected,
          iface.orElse(null), sig.orElse(null), payload);
  }

  private static InboundMethodReturn mapToMethodReturn(Frame frame) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound method return.");
    UInt32 serial = frame.getSerial();
    Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    UInt32 replySerial = getReplySerialFromHeader(headerFields);
    DBusString sender = getSenderFromHeader(headerFields);
    Optional<Signature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundMethodReturn(serial, replySerial, sender,
          sig.orElse(null), payload);
  }

  private static InboundSignal mapToSignal(Frame frame) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound signal.");
    UInt32 serial = frame.getSerial();
    Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    DBusString sender = getSenderFromHeader(headerFields);
    ObjectPath path = getObjectPathFromHeader(headerFields);
    Optional<DBusString> iface = getInterfaceNameFromHeader(headerFields);
    if (iface.isEmpty()) {
      String msg = "Missing interface name in message header.";
      throw new CorruptedFrameException(msg);
    }
    DBusString member = getMemberFromHeader(headerFields);
    Optional<Signature> sig = getSignatureFromHeader(headerFields);
    List<DBusType> payload = null;
    if (sig.isPresent() && frame.getBody().hasRemaining()) {
      payload = decodeFrameBody(frame.getBody(), sig.get());
    }
    return new InboundSignal(serial, sender, path, iface.get(), member,
          sig.orElse(null), payload);
  }

  private static List<DBusType> decodeFrameBody(ByteBuffer body, Signature sig) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Decoding frame body.");
    ArrayList<DBusType> list = new ArrayList<>();
    try {
      if (sig.getQuantity() == 1) {
        int offset = 0;
        var result = DecoderUtils.decode(sig, body, offset);
        list.add(result.getValue());
      } else {
        List<Signature> subSignatures = sig.getChildren();
        int offset = 0;
        for (Signature s : subSignatures) {
          var result = DecoderUtils.decode(s, body, offset);
          list.add(result.getValue());
          offset += result.getConsumedBytes();
        }
      }
    } catch (Exception ex) {
      String failureMsg = "Could not decode frame body.";
      LoggerUtils.error(LOGGER, MARKER, () -> failureMsg, ex);
      throw new CorruptedFrameException(failureMsg, ex);
    }
    list.trimToSize();
    return list;
  }

  @Override
  public void decode(ChannelHandlerContext ctx, Frame frame, List<Object> out) {
    InboundMessage msg = mapFrameToInboundMessage(frame);
    if (msg == null) {
      LoggerUtils.warn(LOGGER, MARKER, () -> "Ignoring inbound message with invalid type.");
    } else {
      LoggerUtils.debug(LOGGER, MARKER, () -> "Decoded an " + msg);
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
