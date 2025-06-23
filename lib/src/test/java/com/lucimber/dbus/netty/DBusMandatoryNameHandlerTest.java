package com.lucimber.dbus.netty;

import com.lucimber.dbus.message.InboundError;
import com.lucimber.dbus.message.InboundMethodReturn;
import com.lucimber.dbus.message.OutboundMethodCall;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    assertEquals(ObjectPath.valueOf("/org/freedesktop/DBus"), helloCall.getObjectPath());
    assertEquals(DBusString.valueOf("Hello"), helloCall.getMember());
  }

  @Test
  void testHelloReplyTriggersNameAcquiredEvent() {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
    OutboundMethodCall sent = channel.readOutbound();
    UInt32 sentSerial = sent.getSerial();

    DBusString name = DBusString.valueOf(":1.101");
    InboundMethodReturn reply = new InboundMethodReturn(
          UInt32.valueOf(0),
          sentSerial,
          SENDER,
          Signature.valueOf("s"), // signature
          List.of(name)
    );

    channel.writeInbound(reply);

    assertEquals(name, channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    assertTrue(channel.pipeline().toMap().values().stream()
          .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
  }

  @Test
  void testHelloReplyWithNoPayloadTriggersFailure() {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
    OutboundMethodCall sent = channel.readOutbound();
    UInt32 sentSerial = sent.getSerial();

    InboundMethodReturn reply = new InboundMethodReturn(
          UInt32.valueOf(0),
          sentSerial,
          SENDER
    );

    channel.writeInbound(reply);

    // No bus name assigned, handler removed, failure event fired
    assertNull(channel.attr(DBusChannelAttribute.ASSIGNED_BUS_NAME).get());
    assertTrue(channel.pipeline().toMap().values().stream()
          .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
  }

  @Test
  void testHelloErrorTriggersFailure() {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
    OutboundMethodCall sent = channel.readOutbound();
    UInt32 sentSerial = sent.getSerial();

    InboundError error = new InboundError(
          UInt32.valueOf(0),
          sentSerial,
          SENDER,
          DBusString.valueOf("org.freedesktop.DBus.Error.Failed")
    );

    channel.writeInbound(error);

    assertTrue(channel.pipeline().toMap().values().stream()
          .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
  }

  @Test
  void testChannelInactiveDuringAwaitingState() {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_AUTH_COMPLETE);
    channel.finish(); // triggers channelInactive

    // Handler should be gone, failure triggered
    assertTrue(channel.pipeline().toMap().values().stream()
          .noneMatch(h -> h instanceof DBusMandatoryNameHandler));
  }
}
