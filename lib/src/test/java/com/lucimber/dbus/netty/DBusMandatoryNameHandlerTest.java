/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class DBusMandatoryNameHandlerTest {

  private EmbeddedChannel channel;
  private static final DBusString SENDER = DBusString.valueOf("org.freedesktop.DBus");

  @BeforeEach
  void setUp() {
  DBusMandatoryNameHandler handler = new DBusMandatoryNameHandler();
  channel = new EmbeddedChannel();
  channel.attr(DBusChannelAttribute.SERIAL_COUNTER).set(new AtomicLong(1));
  channel.pipeline().addLast(handler);
  }

  @Test
  void testHelloCallSentOnPipelineReady() {
  channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);

  Object outbound = channel.readOutbound();
  assertInstanceOf(com.lucimber.dbus.message.OutboundMethodCall.class, outbound);

  OutboundMethodCall helloCall = (OutboundMethodCall) outbound;
  assertEquals(DBusObjectPath.valueOf("/org/freedesktop/DBus"), helloCall.getObjectPath());
  assertEquals(DBusString.valueOf("Hello"), helloCall.getMember());
  }

  @Test
  void testHelloReplyTriggersNameAcquiredEvent() {
  channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
  OutboundMethodCall sent = channel.readOutbound();
  DBusUInt32 sentSerial = sent.getSerial();

  DBusString name = DBusString.valueOf(":1.101");
  InboundMethodReturn reply = InboundMethodReturn.Builder.create()
          .withSerial(DBusUInt32.valueOf(0))
          .withReplySerial(sentSerial)
          .withSender(SENDER)
          .withBody(DBusSignature.valueOf("s"), List.of(name))
          .build();

  channel.writeInbound(reply);

  assertEquals(name, channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
  // Handler should be removed after completion
  assertTrue(channel.pipeline().toMap().values().stream()
          .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
  }

  @Test
  void testHelloReplyWithNoPayloadTriggersFailure() {
  channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
  OutboundMethodCall sent = channel.readOutbound();
  DBusUInt32 sentSerial = sent.getSerial();

  InboundMethodReturn reply = InboundMethodReturn.Builder.create()
          .withSerial(DBusUInt32.valueOf(0))
          .withReplySerial(sentSerial)
          .withSender(SENDER)
          .build();

  channel.writeInbound(reply);

  // No bus name assigned, handler removed after failure
  assertNull(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
  assertTrue(channel.pipeline().toMap().values().stream()
          .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
  }

  @Test
  void testHelloErrorTriggersFailure() {
  channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
  OutboundMethodCall sent = channel.readOutbound();
  DBusUInt32 sentSerial = sent.getSerial();

  List<Object> userEvents = new ArrayList<>();
  channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
      @Override
      public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    userEvents.add(evt);
      }
  });

  InboundError error = InboundError.Builder.create()
          .withSerial(DBusUInt32.valueOf(0))
          .withReplySerial(sentSerial)
          .withSender(SENDER)
          .withErrorName(DBusString.valueOf("org.freedesktop.DBus.Error.Failed"))
          .build();

  channel.writeInbound(error);

  assertTrue(userEvents.contains(DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED));
  }

  @Test
  void testChannelInactiveDuringAwaitingState() {
  channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
  channel.finish(); // triggers channelInactive

  // Handler should be gone when channel becomes inactive and pipeline is torn down
  assertTrue(channel.pipeline().toMap().values().stream()
          .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
  }
}
