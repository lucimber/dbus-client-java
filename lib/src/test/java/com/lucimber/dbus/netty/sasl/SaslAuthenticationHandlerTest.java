/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.netty.DBusChannelEvent;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SaslAuthenticationHandlerTest {

  private EmbeddedChannel channel;

  @BeforeEach
  void setUp() {
    SaslMechanism mockMechanism = new AnonymousSaslMechanism();
    SaslAuthenticationHandler handler = new SaslAuthenticationHandler(List.of(mockMechanism));
    channel = new EmbeddedChannel(handler);
  }

  @Test
  void testSaslStartAuthFlow() {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_NUL_BYTE_SENT);

    Object initialOutbound = channel.readOutbound();
    assertInstanceOf(SaslMessage.class, initialOutbound);
    SaslMessage obSaslMessage = (SaslMessage) initialOutbound;
    assertEquals(SaslCommandName.AUTH, obSaslMessage.getCommandName());

    channel.writeInbound(new SaslMessage(SaslCommandName.REJECTED, "ANONYMOUS"));

    Object nextOutbound = channel.readOutbound();
    assertInstanceOf(SaslMessage.class, nextOutbound);
    SaslMessage nextAuth = (SaslMessage) nextOutbound;

    assertEquals(SaslCommandName.AUTH, nextAuth.getCommandName());
    assertEquals("ANONYMOUS", nextAuth.getCommandArgs().orElse(""));
  }

  @Test
  void testUnexpectedSaslMessageInIdleState() {
    SaslMessage msg = new SaslMessage(SaslCommandName.OK, null);
    channel.writeInbound(msg);
    assertTrue(channel.isOpen());
  }

  @Test
  void testNonSaslMessageReceived() {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_NUL_BYTE_SENT);
    String nonSasl = "NOT_A_SASL_MESSAGE";
    channel.writeInbound(nonSasl);
    assertTrue(channel.finishAndReleaseAll());
  }

  @Test
  void testAuthenticationFailureTriggersFailureEvent() {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_NUL_BYTE_SENT);
    SaslMessage errorMsg = new SaslMessage(SaslCommandName.ERROR, "Error occurred");
    channel.writeInbound(errorMsg);
    assertTrue(channel.finish());
  }

  @Test
  void testChannelInactiveDuringAuthFailsGracefully() throws Exception {
    channel.pipeline().fireUserEventTriggered(DBusChannelEvent.SASL_NUL_BYTE_SENT);
    channel.close().sync();
    assertFalse(channel.isOpen());
  }
}
