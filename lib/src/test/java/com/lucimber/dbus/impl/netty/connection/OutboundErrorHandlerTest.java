package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.impl.netty.ByteOrder;
import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class OutboundErrorHandlerTest {

  @Test
  void encodeOutboundError() {
    final OutboundMessageEncoder handler = new OutboundMessageEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
    final UInt32 replySerialNumber = UInt32.valueOf(1);
    final UInt32 serialNumber = UInt32.valueOf(2);
    final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
    final OutboundError error = new OutboundError(serialNumber, replySerialNumber, destination, errorName);
    assertTrue(channel.writeOutbound(error));
    assertTrue(channel.finish());
    final Frame frame = channel.readOutbound();
    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.ERROR, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    final int bodyLength = 0;
    assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
    Assertions.assertEquals(serialNumber, frame.getSerial(), "Serial number");
  }

  @Test
  void encodeOutboundErrorWithMessage() {
    final OutboundMessageEncoder handler = new OutboundMessageEncoder();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
    final UInt32 replySerialNumber = UInt32.valueOf(1);
    final UInt32 serialNumber = UInt32.valueOf(2);
    final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
    final OutboundError error = new OutboundError(serialNumber, replySerialNumber, destination, errorName);
    final Signature signature = Signature.valueOf("s");
    error.setSignature(signature);
    final List<DBusType> payload = new ArrayList<>();
    payload.add(DBusString.valueOf("Test error message."));
    error.setPayload(payload);
    assertTrue(channel.writeOutbound(error));
    assertTrue(channel.finish());
    final Frame frame = channel.readOutbound();
    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.ERROR, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    final int bodyLength = 24;
    assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
    Assertions.assertEquals(serialNumber, frame.getSerial(), "Serial number");
  }
}
