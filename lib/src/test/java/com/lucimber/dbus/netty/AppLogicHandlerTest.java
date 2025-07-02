package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppLogicHandlerTest {

  private AppLogicHandler handler;
  private Pipeline pipeline;
  private EmbeddedChannel channel;

  @BeforeEach
  void setUp() {
    Connection connection = mock(Connection.class);
    pipeline = mock(Pipeline.class);
    when(connection.getPipeline()).thenReturn(pipeline);

    handler = new AppLogicHandler(Executors.newSingleThreadExecutor(), connection);
    channel = new EmbeddedChannel(handler);
    channel.attr(DBusChannelAttribute.SERIAL_COUNTER).setIfAbsent(new AtomicLong(1));
  }

  @Test
  void testSendMessageWithoutReply() {
    OutboundMethodCall msg = new OutboundMethodCall(
          UInt32.valueOf(1),
          ObjectPath.valueOf("/object"),
          DBusString.valueOf("Ping"),
          false,
          DBusString.valueOf("some.destination"),
          DBusString.valueOf("org.example.Interface"),
          null,
          null
    );

    var outerFuture = handler.writeMessage(msg);

    // Now allow Netty to process scheduled tasks (like the listener)
    channel.runPendingTasks();
    channel.flushOutbound();

    assertTrue(outerFuture.isSuccess(), "Outer future should complete successfully");
    Future<InboundMessage> innerFuture = outerFuture.getNow();
    assertNotNull(innerFuture, "Inner future should be present");
    assertTrue(innerFuture.isDone(), "No reply expected, so inner future should complete successfully");
  }

  @Test
  void testSendMessageWithReplyAndReceiveReturn() {
    UInt32 serial = UInt32.valueOf(101);
    DBusString dst = DBusString.valueOf("some.dst");
    OutboundMethodCall msg = new OutboundMethodCall(
          serial,
          ObjectPath.valueOf("/service"),
          DBusString.valueOf("Echo"),
          true,
          dst,
          DBusString.valueOf("org.example"),
          null,
          null
    );

    var future = handler.writeMessage(msg);

    channel.runPendingTasks();
    channel.flushOutbound();

    // Simulate reply
    InboundMethodReturn reply = new InboundMethodReturn(
          UInt32.valueOf(5), serial, dst, null, null);
    channel.writeInbound(reply);

    var replyFuture = future.getNow();
    replyFuture.addListener(f -> {
      assertTrue(f.isSuccess());
      assertEquals(reply, f.getNow());
    });
  }

  @Test
  void testSendMessageWithReplyAndReceiveError() {
    UInt32 serial = UInt32.valueOf(202);
    DBusString dst = DBusString.valueOf("some.destination");
    OutboundMethodCall msg = new OutboundMethodCall(
          serial,
          ObjectPath.valueOf("/error"),
          DBusString.valueOf("Fail"),
          true,
          dst,
          DBusString.valueOf("org.example"),
          null,
          null
    );

    var future = handler.writeMessage(msg);
    channel.flushOutbound();

    InboundError error = new InboundError(
          UInt32.valueOf(5),
          UInt32.valueOf(202),
          dst,
          DBusString.valueOf("org.freedesktop.DBus.Error.Failed"),
          null,
          null
    );

    channel.writeInbound(error);

    var replyFuture = future.getNow();
    replyFuture.addListener(f -> {
      assertTrue(f.isSuccess()); // Still a success, error handled later by consumer
      assertInstanceOf(InboundError.class, f.getNow());
    });
  }

  @Test
  void testInboundMethodCallGetsErrorResponse() {
    UInt32 serial = UInt32.valueOf(55);
    InboundMethodCall call = new InboundMethodCall(
          serial,
          DBusString.valueOf("some.sender"),
          ObjectPath.valueOf("/unsupported"),
          DBusString.valueOf("Nope"),
          true,
          DBusString.valueOf("org.example"),
          null,
          null
    );

    channel.writeInbound(call);
    channel.runPendingTasks();

    Object outbound = channel.readOutbound();
    assertInstanceOf(OutboundError.class, outbound);
    OutboundError err = (OutboundError) outbound;
    assertEquals(serial, err.getReplySerial());
    assertEquals("org.freedesktop.DBus.Error.NotSupported", err.getErrorName().getDelegate());
  }

  @Test
  void testChannelInactiveFailsAllPending() {
    OutboundMethodCall msg = new OutboundMethodCall(
          UInt32.valueOf(99),
          ObjectPath.valueOf("/fail"),
          DBusString.valueOf("Call"),
          true,
          DBusString.valueOf("some.test.destination"),
          DBusString.valueOf("org.test"),
          null,
          null
    );

    var future = handler.writeMessage(msg);
    channel.close();

    var replyFuture = future.getNow();
    replyFuture.addListener(f ->
          assertTrue(f.isCancelled() || f.cause() instanceof ClosedChannelException));
  }

  @Test
  void testConnectionEvents() {
    handler.userEventTriggered(channel.pipeline().context(handler), DBusChannelEvent.MANDATORY_NAME_ACQUIRED);
    verify(pipeline).propagateConnectionActive();

    handler.userEventTriggered(channel.pipeline().context(handler), DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED);
    verify(pipeline).propagateConnectionInactive();
    assertFalse(channel.isActive());
  }
}
