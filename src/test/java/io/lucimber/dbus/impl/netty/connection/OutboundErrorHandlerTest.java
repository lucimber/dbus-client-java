package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.message.MessageType;
import io.lucimber.dbus.message.OutboundError;
import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.Signature;
import io.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutboundErrorHandlerTest {

    @BeforeEach
    void resetDiagnosticContext() {
        MDC.clear();
    }

    @Test
    public void encodeOutboundError() {
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
        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
        assertEquals(MessageType.ERROR, frame.getType(), "Message type");
        assertTrue(frame.getFlags().isEmpty());
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        final int bodyLength = 0;
        assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
        assertEquals(serialNumber, frame.getSerial(), "Serial number");
    }

    @Test
    public void encodeOutboundErrorWithMessage() {
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
        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
        assertEquals(MessageType.ERROR, frame.getType(), "Message type");
        assertTrue(frame.getFlags().isEmpty());
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        final int bodyLength = 24;
        assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
        assertEquals(serialNumber, frame.getSerial(), "Serial number");
    }
}
