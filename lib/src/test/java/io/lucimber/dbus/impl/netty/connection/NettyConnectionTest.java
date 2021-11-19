package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.connection.Connection;
import io.lucimber.dbus.connection.Pipeline;
import io.lucimber.dbus.connection.PipelineFactory;
import io.lucimber.dbus.message.OutboundMessage;
import io.lucimber.dbus.message.OutboundSignal;
import io.lucimber.dbus.type.DBusString;
import io.lucimber.dbus.type.ObjectPath;
import io.lucimber.dbus.type.UInt32;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

final class NettyConnectionTest {

    @BeforeEach
    void resetDiagnosticContext() {
        MDC.clear();
    }

    @Test
    public void testInboundFailure() {
        final Pipeline pipeline = Mockito.mock(Pipeline.class);
        final PipelineFactory factory = Mockito.mock(PipelineFactory.class);
        Mockito.doAnswer(invocationOnMock -> pipeline).when(factory).create(any(Connection.class));
        final NettyConnection handler = new NettyConnection(factory);
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        final Exception ex = new Exception("test");
        channel.pipeline().fireExceptionCaught(ex);
        Mockito.verify(pipeline).passInboundFailure(ex);
    }

    @Test
    public void testOutboundFailure() {
        final Exception ex = new Exception("test");
        final ChannelOutboundHandler outboundHandler = new WriteFailureOutboundHandler(ex);
        final Pipeline pipeline = Mockito.mock(Pipeline.class);
        final PipelineFactory factory = Mockito.mock(PipelineFactory.class);
        Mockito.doAnswer(invocationOnMock -> pipeline).when(factory).create(any(Connection.class));
        final NettyConnection nettyConnection = new NettyConnection(factory);
        final EmbeddedChannel channel = new EmbeddedChannel(outboundHandler, nettyConnection);
        final UInt32 serial = UInt32.valueOf(1);
        final DBusString destination = DBusString.valueOf("org.example.destination");
        final ObjectPath objectPath = ObjectPath.valueOf("/org/example/path");
        final DBusString interfaceName = DBusString.valueOf("org.example.interface");
        final DBusString signalName = DBusString.valueOf("ExampleSignal");
        final OutboundMessage msg = new OutboundSignal(serial, destination, objectPath, interfaceName, signalName);
        nettyConnection.writeOutboundMessage(msg);
        assertThrows(Exception.class, channel::checkException);
        Mockito.verify(pipeline).passOutboundFailure(ex);
    }
}