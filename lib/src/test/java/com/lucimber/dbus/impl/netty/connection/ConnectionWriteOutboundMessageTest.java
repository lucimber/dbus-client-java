package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.connection.Handler;
import com.lucimber.dbus.connection.HandlerContext;
import com.lucimber.dbus.connection.PipelineFactory;
import com.lucimber.dbus.connection.impl.DefaultPipelineFactory;
import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class ConnectionWriteOutboundMessageTest {

  private static final int TIMEOUT_IN_SECONDS = 5;

  @Test
  void succeedWithInvokingMethod() throws InterruptedException {
    final PipelineFactory chainFactory = new DefaultPipelineFactory();
    final NettyConnection connection = new NettyConnection(chainFactory);
    final AtomicReference<InboundMessage> atomicInboundMessage = new AtomicReference<>();
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final Handler processor = new Handler() {
      @Override
      public void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
        atomicInboundMessage.set(msg);
        countDownLatch.countDown();
      }
    };
    connection.getPipeline().addLast("unit test processor", processor);
    final EmbeddedChannel channel = new EmbeddedChannel(connection);
    // Write method call
    final DBusString destination = DBusString.valueOf("org.freedesktop.DBus");
    final ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");
    final DBusString interfaceName = DBusString.valueOf("org.freedesktop.DBus.Peer");
    final DBusString methodName = DBusString.valueOf("Ping");
    final OutboundMethodCall methodCall =
            new OutboundMethodCall(connection.getNextSerial(), destination, path, methodName);
    methodCall.setInterfaceName(interfaceName);
    connection.writeOutboundMessage(methodCall);
    // Verify method call
    final OutboundMethodCall writtenMethodCall = channel.readOutbound();
    assertEquals(destination, writtenMethodCall.getDestination().orElse(null));
    assertEquals(path.getWrappedValue(), writtenMethodCall.getObjectPath().getWrappedValue());
    assertEquals(interfaceName, writtenMethodCall.getInterfaceName().orElse(null));
    assertEquals(methodName, writtenMethodCall.getName());
    // Write method return
    final UInt32 serialNumber = UInt32.valueOf(1);
    final UInt32 replySerialNumber = methodCall.getSerial();
    final InboundMethodReturn answer = new InboundMethodReturn(serialNumber, replySerialNumber, destination);
    channel.writeInbound(answer);
    // Verify method return
    countDownLatch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    final InboundMessage inboundMessage = atomicInboundMessage.get();
    assertTrue(inboundMessage instanceof InboundMethodReturn);
    final InboundMethodReturn methodReturn = (InboundMethodReturn) inboundMessage;
    assertEquals(destination, methodReturn.getSender(), "Sender");
    assertEquals(methodCall.getSerial(), methodReturn.getReplySerial(), "Serial number");
    assertTrue(methodReturn.getPayload().isEmpty(), "Payload");
  }

  @Test
  void failDueToMethodUnknown() throws InterruptedException {
    final PipelineFactory chainFactory = new DefaultPipelineFactory();
    final NettyConnection connection = new NettyConnection(chainFactory);
    final AtomicReference<InboundMessage> atomicInboundMessage = new AtomicReference<>();
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    final Handler processor = new Handler() {
      @Override
      public void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
        atomicInboundMessage.set(msg);
        countDownLatch.countDown();
      }
    };
    connection.getPipeline().addLast("unit test processor", processor);
    final EmbeddedChannel channel = new EmbeddedChannel(connection);
    // Write method call
    final DBusString destination = DBusString.valueOf("org.freedesktop.DBus");
    final ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");
    final DBusString interfaceName = DBusString.valueOf("org.freedesktop.DBus.Peer");
    final DBusString methodName = DBusString.valueOf("Ping");
    final OutboundMethodCall methodCall =
            new OutboundMethodCall(connection.getNextSerial(), destination, path, methodName);
    methodCall.setInterfaceName(interfaceName);
    connection.writeOutboundMessage(methodCall);
    // Verify method call
    final OutboundMethodCall writtenMethodCall = channel.readOutbound();
    assertEquals(destination, writtenMethodCall.getDestination().orElse(null));
    assertEquals(path.getWrappedValue(), writtenMethodCall.getObjectPath().getWrappedValue());
    assertEquals(interfaceName, writtenMethodCall.getInterfaceName().orElse(null));
    assertEquals(methodName, writtenMethodCall.getName());
    // Write error
    final UInt32 serialNumber = UInt32.valueOf(1);
    final UInt32 replySerialNumber = methodCall.getSerial();
    final DBusString errorName = DBusString.valueOf("org.freedesktop.DBus.Error.UnknownMethod");
    final InboundError answer = new InboundError(serialNumber, replySerialNumber, destination, errorName);
    answer.setSender(destination);
    channel.writeInbound(answer);
    // Verify error
    // Verify method return
    countDownLatch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    final InboundMessage inboundMessage = atomicInboundMessage.get();
    assertTrue(inboundMessage instanceof InboundError);
    final InboundError inboundError = (InboundError) inboundMessage;
    assertEquals(destination, inboundError.getSender(), "Sender");
    assertEquals(methodCall.getSerial(), inboundError.getReplySerial());
    assertTrue(inboundError.getPayload().isEmpty());
  }
}
