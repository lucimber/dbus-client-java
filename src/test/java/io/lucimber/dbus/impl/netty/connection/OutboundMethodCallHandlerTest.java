package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.impl.netty.ByteOrder;
import io.lucimber.dbus.message.MessageType;
import io.lucimber.dbus.message.OutboundMethodCall;
import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.DBusType;
import io.lucimber.dbus.type.ObjectPath;
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

public class OutboundMethodCallHandlerTest {

    @BeforeEach
    void resetDiagnosticContext() {
        MDC.clear();
    }

    @Test
    public void encodeSuccessfully() {
        final OutboundMessageEncoder handler = new OutboundMessageEncoder();
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        final DBusString destination = DBusString.valueOf("io.lucimber.test.destination");
        final UInt32 serialNumber = UInt32.valueOf(1);
        final ObjectPath path = ObjectPath.valueOf("/unit_test");
        final DBusString methodName = DBusString.valueOf("UnitTest");
        final OutboundMethodCall methodCall = new OutboundMethodCall(serialNumber, destination, path, methodName);
        final Signature signature = Signature.valueOf("s");
        methodCall.setSignature(signature);
        final List<DBusType> payload = new ArrayList<>();
        payload.add(DBusString.valueOf("testArg"));
        methodCall.setPayload(payload);
        final DBusString interfaceName = DBusString.valueOf("io.lucimber.test");
        methodCall.setInterfaceName(interfaceName);
        assertTrue(channel.writeOutbound(methodCall));
        assertTrue(channel.finish());
        final Frame frame = channel.readOutbound();
        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
        assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
        assertTrue(frame.getFlags().isEmpty());
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        final int bodyLength = 12;
        assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
        assertEquals(serialNumber, frame.getSerial(), "Serial number");
    }

    @Test
    public void encodeHelloSuccessfully() {
        final OutboundMessageEncoder handler = new OutboundMessageEncoder();
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        final DBusString destination = DBusString.valueOf("org.freedesktop.DBus");
        final UInt32 serialNumber = UInt32.valueOf(1);
        final ObjectPath path = ObjectPath.valueOf("/org/freedesktop/DBus");
        final DBusString interfaceName = DBusString.valueOf("org.freedesktop.DBus");
        final DBusString methodName = DBusString.valueOf("Hello");
        final OutboundMethodCall methodCall = new OutboundMethodCall(serialNumber, destination, path, methodName);
        methodCall.setInterfaceName(interfaceName);
        assertTrue(channel.writeOutbound(methodCall));
        assertTrue(channel.finish());
        final Frame frame = channel.readOutbound();
        assertEquals(ByteOrder.BIG_ENDIAN, frame.getByteOrder(), "Byte order");
        assertEquals(MessageType.METHOD_CALL, frame.getType(), "Message type");
        assertTrue(frame.getFlags().isEmpty());
        assertEquals(1, frame.getProtocolVersion(), "Protocol version");
        final int bodyLength = 0;
        assertEquals(bodyLength, frame.getBody().readableBytes(), "Body length");
        assertEquals(serialNumber, frame.getSerial(), "Serial number");
    }
}
