package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.message.MessageType;
import io.lucimber.dbus.message.OutboundSignal;
import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.ObjectPath;
import io.lucimber.dbus.type.UInt32;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutboundSignalHandlerTest {

    @BeforeEach
    void resetDiagnosticContext() {
        MDC.clear();
    }

    @Test
    public void encodeSuccessfully() {
        final OutboundMessageEncoder handler = new OutboundMessageEncoder();
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        final UInt32 serialNumber = UInt32.valueOf(1);
        final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        final DBusString interfaceName = DBusString.valueOf("io.lucimber.dbus1");
        final ObjectPath path = ObjectPath.valueOf("/test");
        final DBusString signalName = DBusString.valueOf("UnitTest");
        final OutboundSignal signal = new OutboundSignal(serialNumber, destination, path, interfaceName, signalName);
        assertTrue(channel.writeOutbound(signal));
        assertTrue(channel.finish());
        final Frame frame = channel.readOutbound();
        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
        assertEquals(MessageType.SIGNAL, frame.getType(), "Message type");
        assertTrue(frame.getFlags().isEmpty());
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        final int bodyLength = 0;
        assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
        assertEquals(serialNumber, frame.getSerial(), "Serial number");
    }
}
