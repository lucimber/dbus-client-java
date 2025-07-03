/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundSignal;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.nio.ByteOrder;

final class OutboundSignalHandlerTest {

  @Test
  void encodeSuccessfully() {
    UInt32 serialNumber = UInt32.valueOf(1);
    DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
    ObjectPath path = ObjectPath.valueOf("/test");
    DBusString signalName = DBusString.valueOf("UnitTest");
    OutboundSignal signal = OutboundSignal.Builder
            .create()
            .withSerial(serialNumber)
            .withObjectPath(path)
            .withInterface(interfaceName)
            .withMember(signalName)
            .build();

    OutboundMessageEncoder handler = new OutboundMessageEncoder();
    EmbeddedChannel channel = new EmbeddedChannel(handler);
    assertTrue(channel.writeOutbound(signal));
    assertTrue(channel.finish());

    Frame frame = channel.readOutbound();
    Assertions.assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
    Assertions.assertEquals(MessageType.SIGNAL, frame.getType(), "Message type");
    assertTrue(frame.getFlags().isEmpty());
    assertEquals(1, frame.getProtocolVersion(), "Protocol version");
    int bodyLength = 0;
    assertEquals(bodyLength, frame.getBody().remaining(), "Body length");
    assertEquals(serialNumber, frame.getSerial(), "Serial number");
  }
}
