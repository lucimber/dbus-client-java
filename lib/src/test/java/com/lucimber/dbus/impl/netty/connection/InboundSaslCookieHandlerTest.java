package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.sasl.SaslAuthMechanism;
import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslDataMessage;
import com.lucimber.dbus.connection.sasl.SaslErrorMessage;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.connection.sasl.SaslOkMessage;
import com.lucimber.dbus.connection.sasl.SaslRejectedMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;

import java.nio.file.Path;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InboundSaslCookieHandlerTest {

    private static final String IDENTITY = "1234";
    @TempDir
    static Path sharedTempDir;

    @BeforeEach
    void resetDiagnosticContext() {
        MDC.clear();
    }

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
        final byte[] encodedBytes = Base64.getEncoder().encode(incompleteString.getBytes(US_ASCII));
        final String value = new String(encodedBytes, US_ASCII);
        channel.writeInbound(new SaslDataMessage(value));
        final SaslMessage msg = channel.readOutbound();
        assertEquals(SaslCommandName.SHARED_ERROR, msg.getCommandName());
    }
}
