/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.encoder.EncoderResult;
import com.lucimber.dbus.encoder.EncoderUtils;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class FrameDecoderTest {

  private static final int PROTOCOL_VERSION = 1;

  private static ByteBuffer encodeFrameBody(List<DBusType> args, ByteOrder byteOrder) {
    int localByteCount = 0;
    List<ByteBuffer> values = new ArrayList<>();
    for (DBusType dbusObject : args) {
      EncoderResult<ByteBuffer> result = EncoderUtils.encode(dbusObject, localByteCount, byteOrder);
      localByteCount += result.getProducedBytes();
      values.add(result.getBuffer());
    }

    ByteBuffer body = ByteBuffer.allocate(localByteCount);
    for (ByteBuffer bb : values) {
      body.put(bb);
    }
    body.flip();

    return body;
  }

  @BeforeEach
  void resetDiagnosticContext() {
    MDC.clear();
  }

  @Test
  void succeedWithError() {
    FrameDecoder inboundHandler = new FrameDecoder();
    EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);

    Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.ERROR);
    Map<HeaderField, Variant> headerFields = new HashMap<>();
    DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    headerFields.put(HeaderField.SENDER, Variant.valueOf(sender));
    UInt32 replySerial = UInt32.valueOf(1);
    headerFields.put(HeaderField.REPLY_SERIAL, Variant.valueOf(replySerial));
    DBusString errorName = DBusString.valueOf("TestErrorName");
    headerFields.put(HeaderField.ERROR_NAME, Variant.valueOf(errorName));
    frame.setHeaderFields(headerFields);

    assertTrue(channel.writeInbound(frame));
    InboundError inboundMessage = channel.readInbound();

    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(errorName, inboundMessage.getErrorName(), "Error name");
    assertEquals(replySerial, inboundMessage.getReplySerial(), "Reply serial number");
  }

  @Test
  void succeedWithMethodCall() {
    FrameDecoder inboundHandler = new FrameDecoder();
    EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);

    Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.METHOD_CALL);
    Map<HeaderField, Variant> headerFields = new HashMap<>();
    DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    headerFields.put(HeaderField.SENDER, Variant.valueOf(sender));
    ObjectPath path = ObjectPath.valueOf("/io/lucimber/dbus/test");
    headerFields.put(HeaderField.PATH, Variant.valueOf(path));
    DBusString member = DBusString.valueOf("TestMethod");
    headerFields.put(HeaderField.MEMBER, Variant.valueOf(member));
    List<DBusType> args = new ArrayList<>();
    args.add(DBusString.valueOf("test"));
    Signature sig = Signature.valueOf("s");
    headerFields.put(HeaderField.SIGNATURE, Variant.valueOf(sig));
    frame.setHeaderFields(headerFields);
    ByteBuffer bodyBuffer = encodeFrameBody(args, frame.getByteOrder());
    frame.setBody(bodyBuffer);

    assertNotNull(frame.getBody());
    assertTrue(frame.getBody().hasRemaining());

    assertTrue(channel.writeInbound(frame));
    InboundMethodCall inboundMessage = channel.readInbound();

    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(path, inboundMessage.getObjectPath(), "Object path");
    assertEquals(member, inboundMessage.getMember(), "Member");
    assertEquals(args, inboundMessage.getPayload(), "Method arguments");
  }

  @Test
  void succeedWithMethodReturn() {
    FrameDecoder inboundHandler = new FrameDecoder();
    EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);

    Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.METHOD_RETURN);
    Map<HeaderField, Variant> headerFields = new HashMap<>();
    DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    headerFields.put(HeaderField.SENDER, Variant.valueOf(sender));
    UInt32 replySerial = UInt32.valueOf(1);
    headerFields.put(HeaderField.REPLY_SERIAL, Variant.valueOf(replySerial));
    frame.setHeaderFields(headerFields);

    assertTrue(channel.writeInbound(frame));
    InboundMethodReturn inboundMessage = channel.readInbound();

    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(replySerial, inboundMessage.getReplySerial(), "Reply serial number");
  }

  @Test
  void succeedWithSignal() {
    FrameDecoder inboundHandler = new FrameDecoder();
    EmbeddedChannel channel = new EmbeddedChannel(inboundHandler);

    Frame frame = new Frame();
    frame.setByteOrder(ByteOrder.BIG_ENDIAN);
    frame.setProtocolVersion(PROTOCOL_VERSION);
    frame.setSerial(UInt32.valueOf(1));
    frame.setType(MessageType.SIGNAL);
    Map<HeaderField, Variant> headerFields = new HashMap<>();
    DBusString sender = DBusString.valueOf("io.lucimber.dbus.SomeSender");
    headerFields.put(HeaderField.SENDER, Variant.valueOf(sender));
    ObjectPath path = ObjectPath.valueOf("/io/lucimber/dbus/test");
    headerFields.put(HeaderField.PATH, Variant.valueOf(path));
    DBusString iface = DBusString.valueOf("io.lucimber.dbus.SomeInterface");
    headerFields.put(HeaderField.INTERFACE, Variant.valueOf(iface));
    DBusString member = DBusString.valueOf("SomeSignal");
    headerFields.put(HeaderField.MEMBER, Variant.valueOf(member));
    frame.setHeaderFields(headerFields);

    assertTrue(channel.writeInbound(frame));
    InboundSignal inboundMessage = channel.readInbound();

    assertEquals(sender, inboundMessage.getSender(), "Sender");
    assertEquals(path, inboundMessage.getObjectPath(), "Object path");
    assertEquals(iface, inboundMessage.getInterfaceName(), "Interface");
  }
}
