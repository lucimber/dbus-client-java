/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucimber.dbus.message.MessageType;
import com.lucimber.dbus.message.OutboundError;
import com.lucimber.dbus.type.DBusSignature;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.DBusType;
import com.lucimber.dbus.type.DBusUInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class OutboundErrorHandlerTest {

    @Test
    void encodeOutboundError() {
        OutboundMessageEncoder handler = new OutboundMessageEncoder();
        EmbeddedChannel channel = new EmbeddedChannel(handler);

        DBusUInt32 serial = DBusUInt32.valueOf(2);
        DBusUInt32 replySerial = DBusUInt32.valueOf(1);
        DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
        DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
        OutboundError error =
                OutboundError.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .withErrorName(errorName)
                        .withDestination(dst)
                        .build();

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

        DBusUInt32 serial = DBusUInt32.valueOf(2);
        DBusUInt32 replySerial = DBusUInt32.valueOf(1);
        DBusString errorName = DBusString.valueOf("io.lucimber.Error.TestError");
        DBusString dst = DBusString.valueOf("io.lucimber.test.destination");
        DBusSignature sig = DBusSignature.valueOf("s");
        List<DBusType> payload = new ArrayList<>();
        payload.add(DBusString.valueOf("Test error message."));
        OutboundError error =
                OutboundError.Builder.create()
                        .withSerial(serial)
                        .withReplySerial(replySerial)
                        .withErrorName(errorName)
                        .withDestination(dst)
                        .withBody(sig, payload)
                        .build();

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
