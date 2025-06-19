/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.Signature;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OutboundErrorHandlerTest {

  @Test
  void encodeOutboundError() {
    OutboundMessageEncoder handler = new OutboundMessageEncoder();
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    UInt32 serial = UInt32.valueOf(2);
    UInt32 replySerial = UInt32.valueOf(1);
    DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
    DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
    Signature sig = null;
    List<DBusType> payload = null;
    OutboundError error = new OutboundError(serial, replySerial, errorName, dst, sig, payload);

    assertTrue(channel.writeOutbound(error));
    assertTrue(channel.finish());
    Frame frame = channel.readOutbound();

    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.ERROR, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    int bodyLength = 0;
    assertEquals(bodyLength, frame.getBody().remaining(), "Body length");
    Assertions.assertEquals(serial, frame.getSerial(), "Serial number");
  }

  @Test
  void encodeOutboundErrorWithMessage() {
    OutboundMessageEncoder handler = new OutboundMessageEncoder();
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    UInt32 serial = UInt32.valueOf(2);
    UInt32 replySerial = UInt32.valueOf(1);
    DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
    DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
    Signature sig = Signature.valueOf("s");
    List<DBusType> payload = new ArrayList<>();
    payload.add(DBusString.valueOf("Test error message."));
    OutboundError error = new OutboundError(serial, replySerial, errorName, dst, sig, payload);

    assertTrue(channel.writeOutbound(error));
    assertTrue(channel.finish());
    Frame frame = channel.readOutbound();

    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.ERROR, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    int bodyLength = 24;
    assertEquals(bodyLength, frame.getBody().remaining(), "Body length");
    Assertions.assertEquals(serial, frame.getSerial(), "Serial number");
  }
}
