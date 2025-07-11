package com.lucimber.dbus.netty;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.*;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusObjectPath;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppLogicHandlerTest {

  private AppLogicHandler handler;
  private Pipeline pipeline;
  private EmbeddedChannel channel;
  private ExecutorService executorService;

  @BeforeEach
  void setUp() {
    Connection connection = mock(Connection.class);
    pipeline = mock(Pipeline.class);
    when(connection.getPipeline()).thenReturn(pipeline);

    executorService = Executors.newSingleThreadExecutor();
    handler = new AppLogicHandler(executorService, connection);
    channel = new EmbeddedChannel(handler);
    channel.attr(DBusChannelAttribute.SERIAL_COUNTER).setIfAbsent(new AtomicLong(1));
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    if (channel != null) {
      channel.close();
    }
    if (executorService != null) {
      executorService.shutdown();
      if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  @Test
  void testSendMessageWithoutReply() {
    OutboundMethodCall msg = OutboundMethodCall.Builder
            .create()
            .withSerial(DBusUInt32.valueOf(1))
            .withPath(DBusObjectPath.valueOf("/object"))
            .withMember(DBusString.valueOf("Ping"))
            .withDestination(DBusString.valueOf("some.destination"))
            .withInterface(DBusString.valueOf("org.example.Interface"))
            .build();

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
    DBusUInt32 serial = DBusUInt32.valueOf(101);
    DBusString dst = DBusString.valueOf("some.dst");
    OutboundMethodCall msg = OutboundMethodCall.Builder
            .create()
            .withSerial(serial)
            .withPath(DBusObjectPath.valueOf("/service"))
            .withMember(DBusString.valueOf("Echo"))
            .withReplyExpected(true)
            .withDestination(dst)
            .withInterface(DBusString.valueOf("org.example"))
            .build();

    var future = handler.writeMessage(msg);

    channel.runPendingTasks();
    channel.flushOutbound();

    // Simulate reply
    InboundMethodReturn reply = new InboundMethodReturn(
          DBusUInt32.valueOf(5), serial, dst, null, null);
    channel.writeInbound(reply);

    var replyFuture = future.getNow();
    replyFuture.addListener(f -> {
      assertTrue(f.isSuccess());
      assertEquals(reply, f.getNow());
    });
  }

  @Test
  void testSendMessageWithReplyAndReceiveError() {
    DBusUInt32 serial = DBusUInt32.valueOf(202);
    DBusString dst = DBusString.valueOf("some.destination");
    OutboundMethodCall msg = OutboundMethodCall.Builder
            .create()
            .withSerial(serial)
            .withPath(DBusObjectPath.valueOf("/error"))
            .withMember(DBusString.valueOf("Fail"))
            .withReplyExpected(true)
            .withDestination(dst)
            .withInterface(DBusString.valueOf("org.example"))
            .build();

    var future = handler.writeMessage(msg);
    channel.flushOutbound();

    InboundError error = new InboundError(
          DBusUInt32.valueOf(5),
          DBusUInt32.valueOf(202),
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
  void testChannelInactiveFailsAllPending() {
    OutboundMethodCall msg = OutboundMethodCall.Builder
            .create()
            .withSerial(DBusUInt32.valueOf(99))
            .withPath(DBusObjectPath.valueOf("/fail"))
            .withMember(DBusString.valueOf("Call"))
            .withReplyExpected(true)
            .withDestination(DBusString.valueOf("some.test.destination"))
            .withInterface(DBusString.valueOf("org.test"))
            .build();

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

  @Test
  void testMethodCallTimeout() throws InterruptedException {
    // Use a short timeout for testing
    executorService.shutdown();
    executorService = Executors.newSingleThreadExecutor();
    handler = new AppLogicHandler(executorService, mock(Connection.class), 100); // 100ms timeout
    channel = new EmbeddedChannel(handler);
    channel.attr(DBusChannelAttribute.SERIAL_COUNTER).setIfAbsent(new AtomicLong(1));

    OutboundMethodCall msg = OutboundMethodCall.Builder
            .create()
            .withSerial(DBusUInt32.valueOf(123))
            .withPath(DBusObjectPath.valueOf("/timeout"))
            .withMember(DBusString.valueOf("TimeoutMethod"))
            .withReplyExpected(true)
            .withDestination(DBusString.valueOf("some.destination"))
            .withInterface(DBusString.valueOf("org.example"))
            .build();

    var future = handler.writeMessage(msg);
    channel.runPendingTasks();
    channel.flushOutbound();

    // Wait for timeout to occur
    Thread.sleep(150); // Wait longer than timeout
    channel.runScheduledPendingTasks();

    var replyFuture = future.getNow();
    assertTrue(replyFuture.isDone(), "Reply future should be done after timeout");
    assertInstanceOf(TimeoutException.class, replyFuture.cause(), "Should fail with TimeoutException");
  }

  @Test
  void testPerCallTimeoutOverride() throws InterruptedException {
    // Shutdown existing executor and create new handler with short default timeout  
    executorService.shutdown();
    executorService = Executors.newSingleThreadExecutor();
    handler = new AppLogicHandler(executorService, mock(Connection.class), 5000); // 5s default
    channel = new EmbeddedChannel(handler);
    channel.attr(DBusChannelAttribute.SERIAL_COUNTER).setIfAbsent(new AtomicLong(1));

    // Create message with shorter per-call timeout override
    OutboundMethodCall msg = OutboundMethodCall.Builder
            .create()
            .withSerial(DBusUInt32.valueOf(456))
            .withPath(DBusObjectPath.valueOf("/override"))
            .withMember(DBusString.valueOf("FastTimeout"))
            .withReplyExpected(true)
            .withDestination(DBusString.valueOf("some.destination"))
            .withInterface(DBusString.valueOf("org.example"))
            .withTimeout(Duration.ofMillis(50)) // Override with 50ms timeout
            .build();

    var future = handler.writeMessage(msg);
    channel.runPendingTasks();
    channel.flushOutbound();

    // Wait for timeout to occur (should happen in 50ms, not 5000ms)
    Thread.sleep(100); // Wait longer than the override timeout
    channel.runScheduledPendingTasks();

    var replyFuture = future.getNow();
    assertTrue(replyFuture.isDone(), "Reply future should be done after per-call timeout");
    assertInstanceOf(TimeoutException.class, replyFuture.cause(), "Should fail with TimeoutException from override");
    
    // Verify the timeout message mentions the override timeout value
    String errorMessage = replyFuture.cause().getMessage();
    assertTrue(errorMessage.contains("50"), "Error message should mention the override timeout duration");
  }
}
