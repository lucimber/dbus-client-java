/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.netty.ByteOrder;
import com.lucimber.dbus.netty.encoder.EncoderResult;
import com.lucimber.dbus.netty.encoder.EncoderResultImpl;
import com.lucimber.dbus.netty.encoder.EncoderUtils;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.*;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes {@link OutboundMessage}s to the D-Bus marshalling format.
 */
final class OutboundMessageEncoder extends MessageToMessageEncoder<OutboundMessage> {

  private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_OUTBOUND);
  private static final int PROTOCOL_VERSION = 1;

  private static EncoderResult<ByteBuf> encodeBody(final ByteBufAllocator allocator, final List<DBusType> payload) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Encoding message body.");
    final ByteBuf buffer = allocator.buffer();
    int localByteCount = 0;
    for (DBusType value : payload) {
      final EncoderResult<ByteBuf> result = EncoderUtils.encode(value, localByteCount, BYTE_ORDER);
      final ByteBuf tmp = result.getBuffer();
      buffer.writeBytes(tmp);
      tmp.release();
      localByteCount += result.getProducedBytes();
    }
    return new EncoderResultImpl<>(localByteCount, buffer);
  }

  private static MessageType determineMessageType(final OutboundMessage msg) {
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

  private static Map<HeaderField, Variant> buildHeaderFields(final OutboundMessage msg) {
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

  private static Map<HeaderField, Variant> buildHeaderFieldsForSignal(final OutboundSignal msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for signal message.");
    final HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant pathVariant = Variant.valueOf(msg.getObjectPath());
    headerFields.put(HeaderField.PATH, pathVariant);
    final Variant interfaceVariant = Variant.valueOf(msg.getInterfaceName());
    headerFields.put(HeaderField.INTERFACE, interfaceVariant);
    final Variant memberVariant = Variant.valueOf(msg.getMember());
    headerFields.put(HeaderField.MEMBER, memberVariant);
    msg.getDestination().ifPresent(destination -> {
      final Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      final Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static Map<HeaderField, Variant> buildHeaderFieldsForMethodReturn(final OutboundMethodReturn msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for method return message.");
    final HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant replySerialVariant = Variant.valueOf(msg.getReplySerial());
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    msg.getDestination().ifPresent(destination -> {
      final Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      final Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static Map<HeaderField, Variant> buildHeaderFieldsForMethodCall(final OutboundMethodCall msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for method call message.");
    final HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant pathVariant = Variant.valueOf(msg.getObjectPath());
    headerFields.put(HeaderField.PATH, pathVariant);
    final Variant memberVariant = Variant.valueOf(msg.getMember());
    headerFields.put(HeaderField.MEMBER, memberVariant);
    msg.getInterfaceName().ifPresent(interfaceName -> {
      final Variant interfaceVariant = Variant.valueOf(interfaceName);
      headerFields.put(HeaderField.INTERFACE, interfaceVariant);
    });
    msg.getDestination().ifPresent(destination -> {
      final Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      final Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static Map<HeaderField, Variant> buildHeaderFieldsForError(final OutboundError msg) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Building header fields for error message.");
    final HashMap<HeaderField, Variant> headerFields = new HashMap<>();
    final Variant errorNameVariant = Variant.valueOf(msg.getErrorName());
    headerFields.put(HeaderField.ERROR_NAME, errorNameVariant);
    final Variant replySerialVariant = Variant.valueOf(msg.getReplySerial());
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    msg.getDestination().ifPresent(destination -> {
      final Variant destinationVariant = Variant.valueOf(destination);
      headerFields.put(HeaderField.DESTINATION, destinationVariant);
    });
    msg.getSignature().ifPresent(signature -> {
      final Variant signatureVariant = Variant.valueOf(signature);
      headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    });
    return headerFields;
  }

  private static void validatePayload(final List<DBusType> payload, final Signature signature) {
    final boolean matching = isPayloadMatchingWithSignature(payload, signature);
    if (matching) {
      LoggerUtils.debug(LOGGER, () -> "Payload matches signature in message.");
    } else {
      throw new EncoderException("Mismatch between signature and payload.");
    }
  }

  private static boolean isPayloadMatchingWithSignature(final List<DBusType> payload, final Signature signature) {
    if (payload.size() != signature.getQuantity()) {
      return false;
    } else if (signature.getQuantity() == 1) {
      return isObjectMatchingWithSignature(payload.get(0), signature);
    } else {
      final List<Signature> children = signature.getChildren();
      for (int i = 0; i < payload.size(); i++) {
        final boolean matches = isObjectMatchingWithSignature(payload.get(i), children.get(i));
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
  protected void encode(final ChannelHandlerContext ctx, final OutboundMessage msg, final List<Object> out) {
    LoggerUtils.debug(LOGGER, MARKER, () -> "Mapping an outbound message to frame: " + msg);
    msg.getSignature().ifPresent(signature -> validatePayload(msg.getPayload(), signature));
    final Frame frame = new Frame();
    frame.setByteOrder(BYTE_ORDER);
    final MessageType messageType = determineMessageType(msg);
    frame.setType(messageType);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    final EncoderResult<ByteBuf> bodyResult = encodeBody(ctx.alloc(), msg.getPayload());
    frame.setBody(bodyResult.getBuffer());
    frame.setSerial(msg.getSerial());
    final Map<HeaderField, Variant> headerFields = buildHeaderFields(msg);
    frame.setHeaderFields(headerFields);
    out.add(frame);
  }
}
