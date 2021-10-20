package io.lucimber.dbus.impl.netty.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class InboundSaslOneTimeHandlerTest {

    @BeforeEach
    void resetDiagnosticContext() {
        MDC.clear();
    }

    @Test
    void testSendingOfNulByte() {
        final ChannelHandler handler = new SaslNulByteInboundHandler();
        final EmbeddedChannel channel = new EmbeddedChannel(handler);
        final ByteBuf msg = channel.readOutbound();
        assertEquals((byte) 0, msg.readByte());
        assertNull(channel.pipeline().get(SaslNulByteInboundHandler.class));
    }
}
