/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundMethodReturn;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.nio.ByteOrder;

final class OutboundMethodReturnHandlerTest {

  @Test
  void encodeSuccessfully() {
    OutboundMessageEncoder handler = new OutboundMessageEncoder();
    EmbeddedChannel channel = new EmbeddedChannel(handler);

    UInt32 serial = UInt32.valueOf(2);
    UInt32 replySerial = UInt32.valueOf(1);
    DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
    OutboundMethodReturn methodReturn = OutboundMethodReturn.Builder
            .create()
            .withSerial(serial)
            .withReplySerial(replySerial)
            .withDestination(dst)
            .build();

    assertTrue(channel.writeOutbound(methodReturn));
    assertTrue(channel.finish());
    Frame frame = channel.readOutbound();

    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.METHOD_RETURN, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    int bodyLength = 0;
    assertEquals(bodyLength, frame.getBody().remaining(), "Body length");
    Assertions.assertEquals(serial, frame.getSerial(), "Serial number");
  }
}
