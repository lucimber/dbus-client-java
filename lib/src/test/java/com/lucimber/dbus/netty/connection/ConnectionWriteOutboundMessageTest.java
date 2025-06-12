/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import com.lucimber.dbus.connection.Handler;
import com.lucimber.dbus.connection.HandlerContext;
import com.lucimber.dbus.connection.PipelineFactory;
import com.lucimber.dbus.connection.impl.DefaultPipelineFactory;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.*;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class ConnectionWriteOutboundMessageTest {

  private static final int TIMEOUT_IN_SECONDS = 5;

  @Test
  void succeedWithInvokingMethod() throws InterruptedException {
    PipelineFactory chainFactory = new DefaultPipelineFactory();
    NettyConnection connection = new NettyConnection(chainFactory);
    AtomicReference<InboundMessage> atomicInboundMessage = new AtomicReference<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Handler processor = new Handler() {
      @Override
      public void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
        atomicInboundMessage.set(msg);
        countDownLatch.countDown();
      }
    };
    connection.getPipeline().addLast("unit test processor", processor);
    EmbeddedChannel channel = new EmbeddedChannel(connection);

    // Write method call
    UInt32 methodCallSerial = connection.getNextSerial();
    ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");
    DBusString member = DBusString.valueOf("Ping");
    boolean replyExpected = true;
    DBusString dst = DBusString.valueOf("org.freedesktop.DBus");
    DBusString iface = DBusString.valueOf("org.freedesktop.DBus.Peer");
    Signature sig = null;
    List<DBusType> payload = null;
    OutboundMethodCall methodCall = new OutboundMethodCall(methodCallSerial, path, member, replyExpected,
          dst, iface, sig, payload);
    connection.writeOutboundMessage(methodCall);

    // Verify method call
    OutboundMethodCall writtenMethodCall = channel.readOutbound();
    assertEquals(dst, writtenMethodCall.getDestination().orElse(null));
    assertEquals(path.getWrappedValue(), writtenMethodCall.getObjectPath().getWrappedValue());
    assertEquals(iface, writtenMethodCall.getInterfaceName().orElse(null));
    assertEquals(member, writtenMethodCall.getMember());

    // Write method return
    UInt32 methodReturnSerial = UInt32.valueOf(1);
    UInt32 replySerial = methodCall.getSerial();
    InboundMethodReturn answer = new InboundMethodReturn(methodReturnSerial, replySerial, dst);
    channel.writeInbound(answer);

    // Verify method return
    countDownLatch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    InboundMessage inboundMessage = atomicInboundMessage.get();
    assertInstanceOf(InboundMethodReturn.class, inboundMessage);
    InboundMethodReturn methodReturn = (InboundMethodReturn) inboundMessage;
    assertEquals(dst, methodReturn.getSender(), "Sender");
    assertEquals(methodCall.getSerial(), methodReturn.getReplySerial(), "Matching reply serial");
    assertTrue(methodReturn.getPayload().isEmpty(), "Empty payload");
  }

  @Test
  void failDueToMethodUnknown() throws InterruptedException {
    PipelineFactory chainFactory = new DefaultPipelineFactory();
    NettyConnection connection = new NettyConnection(chainFactory);
    AtomicReference<InboundMessage> atomicInboundMessage = new AtomicReference<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Handler processor = new Handler() {
      @Override
      public void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
        atomicInboundMessage.set(msg);
        countDownLatch.countDown();
      }
    };
    connection.getPipeline().addLast("unit test processor", processor);
    EmbeddedChannel channel = new EmbeddedChannel(connection);

    // Write method call
    UInt32 serial = connection.getNextSerial();
    ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");
    DBusString member = DBusString.valueOf("Ping");
    boolean replyExpected = false;
    DBusString dst = DBusString.valueOf("org.freedesktop.DBus");
    DBusString iface = DBusString.valueOf("org.freedesktop.DBus.Peer");
    Signature sig = null;
    List<DBusType> payload = null;
    OutboundMethodCall methodCall = new OutboundMethodCall(serial, path, member, replyExpected,
                dst, iface, sig, payload);
    connection.writeOutboundMessage(methodCall);

    // Verify method call
    OutboundMethodCall writtenMethodCall = channel.readOutbound();
    assertEquals(dst, writtenMethodCall.getDestination().orElse(null));
    assertEquals(path.getWrappedValue(), writtenMethodCall.getObjectPath().getWrappedValue());
    assertEquals(iface, writtenMethodCall.getInterfaceName().orElse(null));
    assertEquals(member, writtenMethodCall.getMember());

    // Write error
    UInt32 errorSerial = UInt32.valueOf(1);
    UInt32 replySerial = methodCall.getSerial();
    DBusString errorName = DBusString.valueOf("org.freedesktop.DBus.Error.UnknownMethod");
    InboundError answer = new InboundError(errorSerial, replySerial, dst, errorName);
    channel.writeInbound(answer);

    // Verify error
    countDownLatch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    InboundMessage inboundMessage = atomicInboundMessage.get();
    assertInstanceOf(InboundError.class, inboundMessage);
    InboundError inboundError = (InboundError) inboundMessage;
    assertEquals(dst, inboundError.getSender(), "Sender");
    assertEquals(methodCall.getSerial(), inboundError.getReplySerial());
    assertTrue(inboundError.getPayload().isEmpty());
  }
}
