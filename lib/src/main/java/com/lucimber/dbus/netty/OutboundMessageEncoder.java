/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.encoder.EncoderResult;
import com.lucimber.dbus.encoder.EncoderResultImpl;
import com.lucimber.dbus.encoder.EncoderUtils;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.message.OutboundSignal;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.Type;
import com.lucimber.dbus.type.TypeUtils;
import com.lucimber.dbus.type.Variant;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Encodes {@link OutboundMessage}s to the D-Bus marshalling format.
 */
final class OutboundMessageEncoder extends MessageToMessageEncoder<OutboundMessage> {

  private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_OUTBOUND);
  private static final int PROTOCOL_VERSION = 1;

  private static EncoderResult<ByteBuffer> encodeBody(List<DBusType> payload) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding message body.");

    int totalSize = 0;
    List<ByteBuffer> values = new ArrayList<>();

    for (DBusType value : payload) {
      EncoderResult<ByteBuffer> result = EncoderUtils.encode(value, totalSize, BYTE_ORDER);
      totalSize += result.getProducedBytes();
      values.add(result.getBuffer());
    }

    ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(OutboundMessageEncoder.BYTE_ORDER);
    for (ByteBuffer bb : values) {
      buffer.put(bb);
    }
    buffer.flip();

    return new EncoderResultImpl<>(totalSize, buffer);
  }

  private static MessageType determineMessageType(OutboundMessage msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Determining message type.");
    if (msg instanceof OutboundError) {
      return MessageType.ERROR;
    } else if (msg instanceof OutboundMethodCall) {
      return MessageType.METHOD_CALL;
    } else if (msg instanceof OutboundMethodReturn) {
      return MessageType.METHOD_RETURN;
    } else if (msg instanceof OutboundSignal) {
      return MessageType.SIGNAL;
    } else {
      throw new EncoderException("Invalid message type.");
    }
  }

  private static Map<HeaderField, Variant> buildHeaderFields(OutboundMessage msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields.");
    if (msg instanceof OutboundError) {
      return buildHeaderFieldsForError((OutboundError) msg);
    } else if (msg instanceof OutboundMethodCall) {
      return buildHeaderFieldsForMethodCall((OutboundMethodCall) msg);
    } else if (msg instanceof OutboundMethodReturn) {
      return buildHeaderFieldsForMethodReturn((OutboundMethodReturn) msg);
    } else if (msg instanceof OutboundSignal) {
      return buildHeaderFieldsForSignal((OutboundSignal) msg);
    } else {
      throw new Error("invalid message type");
    }
  }

  private static Map<HeaderField, Variant> buildHeaderFieldsForSignal(OutboundSignal msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for signal message.");
    HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    Variant pathVariant = Variant.valueOf(msg.getObjectPath());
    headerFields.put(HeaderField.PATH, pathVariant);
    Variant interfaceVariant = Variant.valueOf(msg.getInterfaceName());
    headerFields.put(HeaderField.INTERFACE, interfaceVariant);
    Variant memberVariant = Variant.valueOf(msg.getMember());
    headerFields.put(HeaderField.MEMBER, memberVariant);
    msg.getDestination().ifPresent(destination -> {
      Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static Map<HeaderField, Variant> buildHeaderFieldsForMethodReturn(OutboundMethodReturn msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for method return message.");
    HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    Variant replySerialVariant = Variant.valueOf(msg.getReplySerial());
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    msg.getDestination().ifPresent(destination -> {
      Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static Map<HeaderField, Variant> buildHeaderFieldsForMethodCall(OutboundMethodCall msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for method call message.");
    HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    Variant pathVariant = Variant.valueOf(msg.getObjectPath());
    headerFields.put(HeaderField.PATH, pathVariant);
    Variant memberVariant = Variant.valueOf(msg.getMember());
    headerFields.put(HeaderField.MEMBER, memberVariant);
    msg.getInterfaceName().ifPresent(interfaceName -> {
      Variant interfaceVariant = Variant.valueOf(interfaceName);
      headerFields.put(HeaderField.INTERFACE, interfaceVariant);
    });
    msg.getDestination().ifPresent(destination -> {
      Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static Map<HeaderField, Variant> buildHeaderFieldsForError(OutboundError msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for error message.");
    HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    Variant errorNameVariant = Variant.valueOf(msg.getErrorName());
    headerFields.put(HeaderField.ERROR_NAME, errorNameVariant);
    Variant replySerialVariant = Variant.valueOf(msg.getReplySerial());
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    msg.getDestination().ifPresent(destination -> {
      Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static void validatePayload(List<DBusType> payload, Signature signature) {
    boolean matching = isPayloadMatchingWithSignature(payload, signature);
    if (matching) {
      LoggerUtils.debug(LOGGER, () -> "Payload matches signature in message.");
    } else {
      throw new EncoderException("Mismatch between signature and payload.");
    }
  }

  private static boolean isPayloadMatchingWithSignature(List<DBusType> payload, Signature signature) {
    if (payload.size() != signature.getQuantity()) {
      return false;
    } else if (signature.getQuantity() == 1) {
      return isObjectMatchingWithSignature(payload.get(0), signature);
    } else {
      List<Signature> children = signature.getChildren();
      for (int i = 0; i < payload.size(); i++) {
        boolean matches = isObjectMatchingWithSignature(payload.get(i), children.get(i));
        if (!matches) {
          return false;
        }
      }
      return true;
    }
  }

  private static boolean isObjectMatchingWithSignature(DBusType object, Signature signature) {
    try {
      char c = signature.toString().charAt(0);
      Type signatureType = TypeUtils.getTypeFromChar(c)
              .orElseThrow(() -> new Exception("can not map char to type: " + c));
      return signatureType.getCode() == object.getType().getCode();
    } catch (Exception ex) {
      LOGGER.warn("Object isn't matching with signature.", ex);
      return false;
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, OutboundMessage msg, List<Object> out) {
    LoggerUtils.debug(LOGGER, MARKER, () -> "Mapping an outbound message to frame: " + msg);
    msg.getSignature().ifPresent(signature -> validatePayload(msg.getPayload(), signature));
    Frame frame = new Frame();
    frame.setByteOrder(BYTE_ORDER);
    MessageType messageType = determineMessageType(msg);
    frame.setType(messageType);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    EncoderResult<ByteBuffer> bodyResult = encodeBody(msg.getPayload());
    frame.setBody(bodyResult.getBuffer());
    frame.setSerial(msg.getSerial());
    Map<HeaderField, Variant> headerFields = buildHeaderFields(msg);
    frame.setHeaderFields(headerFields);
    out.add(frame);
  }
}
