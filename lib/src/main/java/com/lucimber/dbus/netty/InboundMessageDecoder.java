/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.codec.decoder.DecoderUtils;
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
import com.lucimber.dbus.util.HeaderFieldExtractor;
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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static Optional<DBusString> getErrorNameFromHeader(
            Map<HeaderField, DBusVariant> headerFields) {
        return HeaderFieldExtractor.extractOptional(
                headerFields, HeaderField.ERROR_NAME, DBusString.class);
    }

    private static Optional<DBusString> getInterfaceNameFromHeader(
            Map<HeaderField, DBusVariant> headerFields) {
        return HeaderFieldExtractor.extractOptional(
                headerFields, HeaderField.INTERFACE, DBusString.class);
    }

    private static DBusString getMemberFromHeader(Map<HeaderField, DBusVariant> headerFields) {
        return HeaderFieldExtractor.extractRequired(
                headerFields, HeaderField.MEMBER, DBusString.class);
    }

    private static DBusObjectPath getObjectPathFromHeader(
            Map<HeaderField, DBusVariant> headerFields) {
        return HeaderFieldExtractor.extractRequired(
                headerFields, HeaderField.PATH, DBusObjectPath.class);
    }

    private static DBusUInt32 getReplySerialFromHeader(Map<HeaderField, DBusVariant> headerFields) {
        return HeaderFieldExtractor.extractRequired(
                headerFields, HeaderField.REPLY_SERIAL, DBusUInt32.class);
    }

    private static Optional<DBusSignature> getSignatureFromHeader(
            Map<HeaderField, DBusVariant> headerFields) {
        return HeaderFieldExtractor.extractOptional(
                headerFields, HeaderField.SIGNATURE, DBusSignature.class);
    }

    private static DBusString getSenderFromHeader(Map<HeaderField, DBusVariant> headerFields) {
        return HeaderFieldExtractor.extractRequired(
                headerFields, HeaderField.SENDER, DBusString.class);
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
        if (sig.isPresent()) {
            if (frame.getBody().hasRemaining()) {
                payload = decodeFrameBody(frame.getBody(), sig.get());
            } else {
                payload = new ArrayList<>(0); // Empty payload, size efficiently
            }
        }
        var builder =
                InboundError.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .withSender(sender)
                        .withErrorName(errorName.get());
        if (sig.isPresent()) {
            builder.withBody(sig.get(), payload);
        }
        return builder.build();
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
        if (sig.isPresent()) {
            if (frame.getBody().hasRemaining()) {
                payload = decodeFrameBody(frame.getBody(), sig.get());
            } else {
                payload = new ArrayList<>(0); // Empty payload, size efficiently
            }
        }
        var builder =
                InboundMethodCall.Builder.create()
                        .withSerial(serial)
                        .withSender(sender)
                        .withObjectPath(path)
                        .withMember(member)
                        .withReplyExpected(replyExpected);
        if (iface.isPresent()) {
            builder.withInterfaceName(iface.get());
        }
        if (sig.isPresent()) {
            builder.withBody(sig.get(), payload);
        }
        return builder.build();
    }

    private static InboundMethodReturn mapToMethodReturn(Frame frame) {
        LOGGER.trace(LoggerUtils.MARSHALLING, "Mapping frame to inbound method return.");
        DBusUInt32 serial = frame.getSerial();
        Map<HeaderField, DBusVariant> headerFields = frame.getHeaderFields();
        DBusUInt32 replySerial = getReplySerialFromHeader(headerFields);
        DBusString sender = getSenderFromHeader(headerFields);
        Optional<DBusSignature> sig = getSignatureFromHeader(headerFields);
        List<DBusType> payload = null;
        if (sig.isPresent()) {
            if (frame.getBody().hasRemaining()) {
                payload = decodeFrameBody(frame.getBody(), sig.get());
            } else {
                payload = new ArrayList<>(0); // Empty payload, size efficiently
            }
        }
        var builder =
                InboundMethodReturn.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .withSender(sender);
        if (sig.isPresent()) {
            builder.withBody(sig.get(), payload);
        }
        return builder.build();
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
        if (sig.isPresent()) {
            if (frame.getBody().hasRemaining()) {
                payload = decodeFrameBody(frame.getBody(), sig.get());
            } else {
                payload = new ArrayList<>(0); // Empty payload, size efficiently
            }
        }
        var builder =
                InboundSignal.Builder.create()
                        .withSerial(serial)
                        .withSender(sender)
                        .withObjectPath(path)
                        .withInterfaceName(iface.get())
                        .withMember(member);
        if (sig.isPresent()) {
            builder.withBody(sig.get(), payload);
        }
        return builder.build();
    }

    private static List<DBusType> decodeFrameBody(ByteBuffer body, DBusSignature sig) {
        LOGGER.trace(LoggerUtils.MARSHALLING, "Decoding frame body.");
        // Pre-size the list based on signature quantity for better memory efficiency
        int expectedSize = sig.getQuantity() > 0 ? sig.getQuantity() : 1;
        ArrayList<DBusType> list = new ArrayList<>(expectedSize);
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
        LOGGER.debug(
                LoggerUtils.MARSHALLING,
                "Decoding frame: type={}, serial={}",
                frame.getType(),
                frame.getSerial());
        InboundMessage msg = mapFrameToInboundMessage(frame);
        if (msg == null) {
            LOGGER.warn(
                    LoggerUtils.MARSHALLING,
                    "Failed to decode frame with invalid type: {}",
                    frame.getType());
        } else {
            LOGGER.debug(
                    LoggerUtils.MARSHALLING,
                    "Decoded {} with serial={}",
                    msg.getClass().getSimpleName(),
                    msg.getSerial());
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
