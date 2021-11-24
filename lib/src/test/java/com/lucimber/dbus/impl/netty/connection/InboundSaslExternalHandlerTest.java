package com.lucimber.dbus.impl.netty.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

final class InboundSaslExternalHandlerTest {

  private static final String IDENTITY = "1234";

  @Test
  void writeCorrectAuthMessage() {
    final ChannelHandler handler = new SaslExternalInboundHandler(IDENTITY);
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_AUTH, msg.getCommandName());
    assertTrue(msg.getCommandValue().orElse("is missing")
            .startsWith(SaslAuthMechanism.EXTERNAL.toString()));
  }

  @Test
  void receiveOkWhileWaitingForOk() {
    final ChannelHandler handler = new SaslExternalInboundHandler(IDENTITY);
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslOkMessage("1234cafe5babe"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_BEGIN, msg.getCommandName());
  }

  @Test
  void receiveRejectedWhileWaitingForOk() {
    final ChannelHandler handler = new SaslExternalInboundHandler(IDENTITY);
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslRejectedMessage());
    assertFalse(channel.isOpen());
  }

  @Test
  void receiveDataWhileWaitingForOk() {
    final ChannelHandler handler = new SaslExternalInboundHandler(IDENTITY);
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
    final ChannelHandler handler = new SaslExternalInboundHandler(IDENTITY);
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslErrorMessage("unit test"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_CANCEL, msg.getCommandName());
    channel.writeInbound(new SaslRejectedMessage("unit test"));
    assertFalse(channel.isOpen());
  }
}
