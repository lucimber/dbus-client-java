/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.impl.netty.encoder.EncoderResult;
import com.lucimber.dbus.impl.netty.encoder.EncoderResultImpl;
import com.lucimber.dbus.impl.netty.encoder.EncoderUtils;
import com.lucimber.dbus.message.HeaderField;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMethodCall;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.InboundSignal;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.protocol.types.DBusString;
import com.lucimber.dbus.protocol.types.DBusType;
import com.lucimber.dbus.protocol.types.ObjectPath;
import com.lucimber.dbus.protocol.types.Signature;
import com.lucimber.dbus.protocol.types.UInt32;
import com.lucimber.dbus.protocol.types.Variant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

final class FrameDecoderTest {

  private static final int PROTOCOL_VERSION = 1;

  private static EncoderResult<ByteBuf> encodeFrameBody(final List<DBusType> args, final ByteOrder byteOrder) {
    final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
    final ByteBuf body = allocator.buffer();
    int localByteCount = 0;
    for (DBusType dbusObject : args) {
      final EncoderResult<ByteBuf> result = EncoderUtils.encode(dbusObject, localByteCount, byteOrder);
      localByteCount += result.getProducedBytes();
      final ByteBuf buffer = result.getBuffer();
      body.writeBytes(buffer);
      buffer.release();
    }
    return new EncoderResultImpl<>(localByteCount, body);
  }

  @BeforeEach
  void resetDiagnosticContext() {
    MDC.clear();
  }

  @Test
  void succeedWithError() {
    final FrameDecoder inboundHandler = new FrameDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    final Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.ERROR);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    final Variant senderVariant = Variant.valueOf(sender);
    headerFields.put(HeaderField.SENDER, senderVariant);
    final UInt32 replySerial = UInt32.valueOf(1);
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    final DBusString errorName = DBusString.valueOf("TestErrorName");
    final Variant errorNameVariant = Variant.valueOf(errorName);
    headerFields.put(HeaderField.ERROR_NAME, errorNameVariant);
    frame.setHeaderFields(headerFields);
    assertTrue(channel.writeInbound(frame));
    final InboundError inboundMessage = channel.readInbound();
    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(errorName, inboundMessage.getName(), "Error name");
    assertEquals(replySerial, inboundMessage.getReplySerial(), "Reply serial number");
  }

  @Test
  void succeedWithMethodCall() {
    final FrameDecoder inboundHandler = new FrameDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    final Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.METHOD_CALL);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    final Variant senderVariant = Variant.valueOf(sender);
    headerFields.put(HeaderField.SENDER, senderVariant);
    final ObjectPath path = ObjectPath.valueOf("/io/lucimber/dbus/test");
    final Variant pathVariant = Variant.valueOf(path);
    headerFields.put(HeaderField.PATH, pathVariant);
    final DBusString member = DBusString.valueOf("TestMethod");
    final Variant memberVariant = Variant.valueOf(member);
    headerFields.put(HeaderField.MEMBER, memberVariant);
    final List<DBusType> args = new ArrayList<>();
    args.add(DBusString.valueOf("test"));
    final Signature signature = Signature.valueOf("s");
    final Variant signatureVariant = Variant.valueOf(signature);
    headerFields.put(HeaderField.SIGNATURE, signatureVariant);
    frame.setHeaderFields(headerFields);
    final EncoderResult<ByteBuf> bodyResult = encodeFrameBody(args, frame.getByteOrder());
    frame.setBody(bodyResult.getBuffer());
    assertTrue(channel.writeInbound(frame));
    final InboundMethodCall inboundMessage = channel.readInbound();
    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(path, inboundMessage.getObjectPath(), "Object path");
    assertEquals(member, inboundMessage.getName(), "Member");
    assertEquals(args, inboundMessage.getPayload(), "Method arguments");
  }

  @Test
  void succeedWithMethodReturn() {
    final FrameDecoder inboundHandler = new FrameDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    final Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.METHOD_RETURN);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    final Variant senderVariant = Variant.valueOf(sender);
    headerFields.put(HeaderField.SENDER, senderVariant);
    final UInt32 replySerial = UInt32.valueOf(1);
    final Variant replySerialVariant = Variant.valueOf(replySerial);
    headerFields.put(HeaderField.REPLY_SERIAL, replySerialVariant);
    frame.setHeaderFields(headerFields);
    assertTrue(channel.writeInbound(frame));
    final InboundMethodReturn inboundMessage = channel.readInbound();
    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(replySerial, inboundMessage.getReplySerial(), "Reply serial number");
  }

  @Test
  void succeedWithSignal() {
    final FrameDecoder inboundHandler = new FrameDecoder();
    final EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);
    final Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.SIGNAL);
    final Map<HeaderField, Variant> headerFields = new HashMap<>();
    final DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    final Variant senderVariant = Variant.valueOf(sender);
    headerFields.put(HeaderField.SENDER, senderVariant);
    final ObjectPath path = ObjectPath.valueOf("/io/lucimber/dbus/test");
    final Variant pathVariant = Variant.valueOf(path);
    headerFields.put(HeaderField.PATH, pathVariant);
    final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus.SomeInterface");
    final Variant interfaceVariant = Variant.valueOf(interfaceName);
    headerFields.put(HeaderField.INTERFACE, interfaceVariant);
    final DBusString member = DBusString.valueOf("SomeSignal");
    final Variant memberVariant = Variant.valueOf(member);
    headerFields.put(HeaderField.MEMBER, memberVariant);
    frame.setHeaderFields(headerFields);
    assertTrue(channel.writeInbound(frame));
    final InboundSignal inboundMessage = channel.readInbound();
    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(path, inboundMessage.getObjectPath(), "Object path");
    assertEquals(interfaceName, inboundMessage.getInterfaceName(), "Interface");
  }
}
