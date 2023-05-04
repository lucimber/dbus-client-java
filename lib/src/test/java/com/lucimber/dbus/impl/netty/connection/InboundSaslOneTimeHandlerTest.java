/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

final class InboundSaslOneTimeHandlerTest {

  @Test
  void testSendingOfNulByte() {
    final ChannelHandler handler = new SaslNulByteInboundHandler();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final ByteBuf msg = channel.readOutbound();
    assertEquals((byte) 0, msg.readByte());
    assertNull(channel.pipeline().get(SaslNulByteInboundHandler.class));
  }
}
