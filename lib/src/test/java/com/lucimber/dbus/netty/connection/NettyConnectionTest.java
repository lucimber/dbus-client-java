/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import static org.mockito.ArgumentMatchers.any;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.connection.PipelineFactory;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.message.OutboundSignal;
import com.lucimber.dbus.type.DBusString;
import com.lucimber.dbus.type.ObjectPath;
import com.lucimber.dbus.type.UInt32;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class NettyConnectionTest {

  @Test
  void testInboundFailure() {
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
  void testOutboundFailure() {
    final Exception ex = new Exception("test");
    final ChannelOutboundHandler outboundHandler = new WriteFailureOutboundHandler(ex);
    final Pipeline pipeline = Mockito.mock(Pipeline.class);
    final PipelineFactory factory = Mockito.mock(PipelineFactory.class);
    Mockito.doAnswer(invocationOnMock -> pipeline).when(factory).create(any(Connection.class));
    final NettyConnection nettyConnection = new NettyConnection(factory);
    final EmbeddedChannel channel = new EmbeddedChannel(outboundHandler, nettyConnection);
    final UInt32 serial = UInt32.valueOf(1);
    final ObjectPath objectPath = ObjectPath.valueOf("/org/example/path");
    final DBusString interfaceName = DBusString.valueOf("org.example.interface");
    final DBusString signalName = DBusString.valueOf("ExampleSignal");
    final OutboundMessage msg = new OutboundSignal(serial, objectPath, interfaceName, signalName);
    nettyConnection.writeOutboundMessage(msg);
    channel.checkException(); // Must not rethrow exception
    Mockito.verify(pipeline).passOutboundFailure(ex);
  }
}
