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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InboundSaslCookieHandlerTest {

  private static final String IDENTITY = "1234";
  @TempDir
  private static Path sharedTempDir;

  @Test
  void writeCorrectAuthMessage() {
    final ChannelHandler handler = new SaslCookieInboundHandler(IDENTITY, sharedTempDir.toString());
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_AUTH, msg.getCommandName());
    assertTrue(msg.getCommandValue().isPresent());
    assertTrue(msg.getCommandValue().get().startsWith(SaslAuthMechanism.COOKIE.toString()));
  }

  @Test
  void receiveRejectedWhileWaitingForData() {
    final ChannelHandler handler = new SaslCookieInboundHandler(IDENTITY, sharedTempDir.toString());
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslRejectedMessage("unit test"));
    assertFalse(channel.isOpen());
  }

  @Test
  void receiveErrorWhileWaitingForData() {
    final ChannelHandler handler = new SaslCookieInboundHandler(IDENTITY, sharedTempDir.toString());
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslErrorMessage("unit test"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_CANCEL, msg.getCommandName());
    channel.writeInbound(new SaslRejectedMessage("unit test"));
    assertFalse(channel.isOpen());
  }

  @Test
  void receiveOkWhileWaitingForData() {
    final ChannelHandler handler = new SaslCookieInboundHandler(IDENTITY, sharedTempDir.toString());
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslOkMessage("1234cafe5babe"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.CLIENT_BEGIN, msg.getCommandName());
  }

  @Test
  void receiveInvalidBase64StringWhileWaitingForData() {
    final ChannelHandler handler = new SaslCookieInboundHandler(IDENTITY, sharedTempDir.toString());
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    channel.writeInbound(new SaslDataMessage("gibberish"));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.SHARED_ERROR, msg.getCommandName());
  }

  @Test
  void receiveIncompleteDataValuesWhileWaitingForData() {
    final ChannelHandler handler = new SaslCookieInboundHandler(IDENTITY, sharedTempDir.toString());
    final EmbeddedChannel channel = new EmbeddedChannel(handler);
    channel.readOutbound();
    final String incompleteString = "one two";
    final byte[] encodedBytes = Base64.getEncoder().encode(incompleteString.getBytes(StandardCharsets.US_ASCII));
    final String value = new String(encodedBytes, StandardCharsets.US_ASCII);
    channel.writeInbound(new SaslDataMessage(value));
    final SaslMessage msg = channel.readOutbound();
    assertEquals(SaslCommandName.SHARED_ERROR, msg.getCommandName());
  }
}
