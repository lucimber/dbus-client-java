package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.impl.netty.decoder.DecoderResult;
import com.lucimber.dbus.impl.netty.decoder.DecoderUtils;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.Message;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import com.lucimber.dbus.type.Variant;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * An inbound handler that decodes message frames into typed messages.
 *
 * @see Frame
 * @see Message
 */
final class FrameDecoder extends MessageToMessageDecoder<Frame> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_CONNECTION_INBOUND);

  private static Optional<DBusString> getErrorNameFromHeader(final Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting signature from message header.");
    final Variant variant = headerFields.get(HeaderField.ERROR_NAME);
    if (variant == null) {
      return Optional.empty();
    }
    final DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return Optional.of((DBusString) variantValue);
    } else {
      final String msg = "Error name in message header is of wrong type.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static Optional<DBusString> getInterfaceNameFromHeader(final Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting interface name from message header.");
    final Variant variant = headerFields.get(HeaderField.INTERFACE);
    if (variant == null) {
      return Optional.empty();
    }
    final DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return Optional.of((DBusString) variantValue);
    } else {
      final String msg = "Interface name in message header is of wrong type.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static DBusString getMemberFromHeader(final Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting member from message header.");
    final Variant variant = headerFields.get(HeaderField.MEMBER);
    if (variant == null) {
      final String msg = "Missing member in message header.";
      throw new CorruptedFrameException(msg);
    }
    final DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return (DBusString) variantValue;
    } else {
      final String msg = "Member in message header is of wrong type.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static ObjectPath getObjectPathFromHeader(final Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting object path from message header.");
    final Variant variant = headerFields.get(HeaderField.PATH);
    if (variant == null) {
      final String msg = "Missing object path in message header.";
      throw new CorruptedFrameException(msg);
    }
    final DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof ObjectPath) {
      return (ObjectPath) variantValue;
    } else {
      final String msg = "Object path in message header must be an ObjectPath.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static UInt32 getReplySerialFromHeader(final Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting reply serial from message header.");
    final Variant variant = headerFields.get(HeaderField.REPLY_SERIAL);
    if (variant == null) {
      final String msg = "Missing reply serial in message header.";
      throw new CorruptedFrameException(msg);
    }
    final DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof UInt32) {
      return (UInt32) variantValue;
    } else {
      final String msg = "Reply serial in message header must be an UINT32.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static Optional<Signature> getSignatureFromHeader(final Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting signature from message header.");
    final Variant variant = headerFields.get(HeaderField.SIGNATURE);
    if (variant == null) {
      return Optional.empty();
    }
    final DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof Signature) {
      return Optional.of((Signature) variantValue);
    } else {
      final String msg = "Signature in message header be a signature.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static DBusString getSenderFromHeader(final Map<HeaderField, Variant> headerFields) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Getting sender from message header.");
    final Variant variant = headerFields.get(HeaderField.SENDER);
    if (variant == null) {
      final String msg = "Missing sender in message header.";
      throw new CorruptedFrameException(msg);
    }
    final DBusType variantValue = variant.getDelegate();
    if (variantValue instanceof DBusString) {
      return (DBusString) variantValue;
    } else {
      final String msg = "Sender in message header must be a string.";
      throw new CorruptedFrameException(msg);
    }
  }

  private static InboundError mapToError(final Frame frame) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound error.");
    final UInt32 serial = frame.getSerial();
    final Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    final UInt32 replySerial = getReplySerialFromHeader(headerFields);
    final DBusString sender = getSenderFromHeader(headerFields);
    final Optional<DBusString> optionalName = getErrorNameFromHeader(headerFields);
    if (!optionalName.isPresent()) {
      final String msg = "Missing error name in message header.";
      throw new CorruptedFrameException(msg);
    }
    final InboundError error = new InboundError(serial, replySerial, sender, optionalName.get());
    final Optional<Signature> optionalSignature = getSignatureFromHeader(headerFields);
    optionalSignature.ifPresent(s -> {
      error.setSignature(s);
      if (frame.getBody().readableBytes() > 0) {
        final List<DBusType> payload = decodeFrameBody(frame.getBody(), s, frame.getByteOrder());
        error.setPayload(payload);
      }
    });
    return error;
  }

  private static InboundMethodCall mapToMethodCall(final Frame frame) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound method call.");
    final UInt32 serial = frame.getSerial();
    final Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    final DBusString sender = getSenderFromHeader(headerFields);
    final ObjectPath path = getObjectPathFromHeader(headerFields);
    final DBusString name = getMemberFromHeader(headerFields);
    final InboundMethodCall methodCall = new InboundMethodCall(serial, sender, path, name);
    final Optional<DBusString> optionalInterface = getInterfaceNameFromHeader(headerFields);
    optionalInterface.ifPresent(methodCall::setInterfaceName);
    final Optional<Signature> optionalSignature = getSignatureFromHeader(headerFields);
    optionalSignature.ifPresent(s -> {
      methodCall.setSignature(s);
      if (frame.getBody().readableBytes() > 0) {
        final List<DBusType> payload = decodeFrameBody(frame.getBody(), s, frame.getByteOrder());
        methodCall.setPayload(payload);
      }
    });
    return methodCall;
  }

  private static InboundMethodReturn mapToMethodReturn(final Frame frame) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound method return.");
    final UInt32 serial = frame.getSerial();
    final Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    final UInt32 replySerial = getReplySerialFromHeader(headerFields);
    final DBusString sender = getSenderFromHeader(headerFields);
    final InboundMethodReturn methodReturn = new InboundMethodReturn(serial, replySerial, sender);
    final Optional<Signature> optionalSignature = getSignatureFromHeader(headerFields);
    optionalSignature.ifPresent(s -> {
      methodReturn.setSignature(s);
      if (frame.getBody().readableBytes() > 0) {
        final List<DBusType> payload = decodeFrameBody(frame.getBody(), s, frame.getByteOrder());
        methodReturn.setPayload(payload);
      }
    });
    return methodReturn;
  }

  private static InboundSignal mapToSignal(final Frame frame) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Mapping frame to inbound signal.");
    final UInt32 serial = frame.getSerial();
    final Map<HeaderField, Variant> headerFields = frame.getHeaderFields();
    final DBusString sender = getSenderFromHeader(headerFields);
    final ObjectPath path = getObjectPathFromHeader(headerFields);
    final Optional<DBusString> optionalInterface = getInterfaceNameFromHeader(headerFields);
    if (!optionalInterface.isPresent()) {
      final String msg = "Missing interface name in message header.";
      throw new CorruptedFrameException(msg);
    }
    final DBusString signalName = getMemberFromHeader(headerFields);
    final InboundSignal signal = new InboundSignal(serial, sender, path, optionalInterface.get(), signalName);
    final Optional<Signature> optionalSignature = getSignatureFromHeader(headerFields);
    optionalSignature.ifPresent(s -> {
      signal.setSignature(s);
      if (frame.getBody().readableBytes() > 0) {
        final List<DBusType> payload = decodeFrameBody(frame.getBody(), s, frame.getByteOrder());
        signal.setPayload(payload);
      }
    });
    return signal;
  }

  private static List<DBusType> decodeFrameBody(final ByteBuf body, final Signature signature,
                                                final ByteOrder byteOrder) {
    LoggerUtils.trace(LOGGER, MARKER, () -> "Decoding frame body.");
    final ArrayList<DBusType> list = new ArrayList<>();
    try {
      if (signature.getQuantity() == 1) {
        final int offset = 0;
        final DecoderResult<? extends DBusType> result =
                DecoderUtils.decode(signature, body, offset, byteOrder);
        list.add(result.getValue());
      } else {
        final List<Signature> subSignatures = signature.getChildren();
        int offset = 0;
        for (Signature s : subSignatures) {
          final DecoderResult<? extends DBusType> result =
                  DecoderUtils.decode(s, body, offset, byteOrder);
          list.add(result.getValue());
          offset += result.getConsumedBytes();
        }
      }
    } catch (Exception ex) {
      final String failureMsg = "Could not decode frame body.";
      LoggerUtils.error(LOGGER, MARKER, () -> failureMsg, ex);
      throw new CorruptedFrameException(failureMsg, ex);
    }
    body.release();
    list.trimToSize();
    return list;
  }

  @Override
  public void decode(final ChannelHandlerContext ctx, final Frame frame, final List<Object> out) {
    final InboundMessage msg = mapFrameToInboundMessage(frame);
    if (msg == null) {
      LoggerUtils.warn(LOGGER, MARKER, () -> "Ignoring inbound message with invalid type.");
    } else {
      LoggerUtils.debug(LOGGER, MARKER, () -> "Decoded frame to inbound message: " + msg);
      out.add(msg);
    }
  }

  private InboundMessage mapFrameToInboundMessage(final Frame frame) {
    switch (frame.getType()) {
      default:
        return null;
      case ERROR:
        return mapToError(frame);
      case METHOD_CALL:
        return mapToMethodCall(frame);
      case METHOD_RETURN:
        return mapToMethodReturn(frame);
      case SIGNAL:
        return mapToSignal(frame);
    }
  }
}
