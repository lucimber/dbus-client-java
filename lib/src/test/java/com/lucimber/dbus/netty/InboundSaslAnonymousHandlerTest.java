/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.lucimber.dbus.connection.sasl.SaslAuthMechanism;
import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslDataMessage;
import com.lucimber.dbus.connection.sasl.SaslErrorMessage;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.connection.sasl.SaslOkMessage;
import com.lucimber.dbus.connection.sasl.SaslRejectedMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

final class InboundSaslAnonymousHandlerTest {

  @Test
  void writeCorrectAuthMessage() {
    final ChannelHandler handler = new SaslAnonymousInboundHandler();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final SaslMessage authMsg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_AUTH, authMsg.getCommandName());
    assertEquals(SaslAuthMechanism.ANONYMOUS.toString(), authMsg.getCommandValue().orElse("is missing"));
  }

  @Test
  void receiveOkWhileWaitingForOk() {
    final ChannelHandler handler = new SaslAnonymousInboundHandler();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslOkMessage("1234cafe5babe"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_BEGIN, msg.getCommandName());
  }

  @Test
  void receiveRejectedWhileWaitingForOk() {
    final ChannelHandler handler = new SaslAnonymousInboundHandler();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslRejectedMessage());
    assertFalse(channel.isOpen());
  }

  @Test
  void receiveDataWhileWaitingForOk() {
    final ChannelHandler handler = new SaslAnonymousInboundHandler();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslDataMessage("gibberish"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_CANCEL, msg.getCommandName());
    channel.writeInbound(new SaslRejectedMessage("unit test"));
    assertFalse(channel.isOpen());
  }

  @Test
  void receiveErrorWhileWaitingForOk() {
    final ChannelHandler handler = new SaslAnonymousInboundHandler();
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslErrorMessage("unit test"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_CANCEL, msg.getCommandName());
    channel.writeInbound(new SaslRejectedMessage("unit test"));
    assertFalse(channel.isOpen());
  }
}
