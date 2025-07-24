/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.netty.sasl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lucimber.dbus.netty.DBusChannelEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SaslInitiationHandlerTest {

    @Mock private ChannelHandlerContext context;

    @Mock private Channel channel;

    @Mock private ChannelPipeline pipeline;

    @Mock private ChannelFuture writeFuture;

    private SaslInitiationHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new SaslInitiationHandler();

        when(context.channel()).thenReturn(channel);
        when(context.pipeline()).thenReturn(pipeline);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("localhost", 1234));
        when(context.writeAndFlush(any())).thenReturn(writeFuture);
    }

    @Test
    void testChannelActiveSuccess() {
        // Setup successful write
        when(writeFuture.isSuccess()).thenReturn(true);

        handler.channelActive(context);

        // Verify NUL byte is written
        ArgumentCaptor<ByteBuf> bufCaptor = ArgumentCaptor.forClass(ByteBuf.class);
        verify(context).writeAndFlush(bufCaptor.capture());

        ByteBuf sentBuf = bufCaptor.getValue();
        byte[] sentBytes = new byte[sentBuf.readableBytes()];
        sentBuf.getBytes(0, sentBytes);
        assertArrayEquals(new byte[] {0}, sentBytes);

        // Verify listener is added
        ArgumentCaptor<GenericFutureListener> listenerCaptor =
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(writeFuture).addListener(listenerCaptor.capture());

        // Execute the listener to simulate successful write
        GenericFutureListener<ChannelFuture> listener = listenerCaptor.getValue();
        try {
            listener.operationComplete(writeFuture);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        // Verify event is fired and handler is removed
        verify(context).fireUserEventTriggered(DBusChannelEvent.SASL_NUL_BYTE_SENT);
        verify(pipeline).remove(handler);
    }

    @Test
    void testChannelActiveFailure() {
        // Setup failed write
        when(writeFuture.isSuccess()).thenReturn(false);
        Exception cause = new Exception("Write failed");
        when(writeFuture.cause()).thenReturn(cause);

        handler.channelActive(context);

        // Verify listener is added
        ArgumentCaptor<GenericFutureListener> listenerCaptor =
                ArgumentCaptor.forClass(GenericFutureListener.class);
        verify(writeFuture).addListener(listenerCaptor.capture());

        // Execute the listener to simulate failed write
        GenericFutureListener<ChannelFuture> listener = listenerCaptor.getValue();
        try {
            listener.operationComplete(writeFuture);
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }

        // Verify channel is closed on failure
        verify(context).close();
        // Verify event is NOT fired and handler is NOT removed
        verify(context, never()).fireUserEventTriggered(any());
        verify(pipeline, never()).remove(any(ChannelHandler.class));
    }

    @Test
    void testUserEventTriggeredWithReconnection() throws Exception {
        handler.userEventTriggered(context, DBusChannelEvent.RECONNECTION_STARTING);

        // Verify event is propagated
        verify(context).fireUserEventTriggered(DBusChannelEvent.RECONNECTION_STARTING);
    }

    @Test
    void testUserEventTriggeredWithOtherEvent() throws Exception {
        Object customEvent = new Object();
        handler.userEventTriggered(context, customEvent);

        // Verify event is propagated
        verify(context).fireUserEventTriggered(customEvent);
    }

    @Test
    void testExceptionCaught() {
        Exception exception = new Exception("Test exception");

        handler.exceptionCaught(context, exception);

        // Verify channel is closed
        verify(context).close();
    }

    @Test
    void testReset() {
        // Reset method should complete without errors
        handler.reset();
        // Since reset has no state to clear, we just verify it doesn't throw
    }

    @Test
    void testMultipleChannelActiveCallsBeforeCompletion() {
        // Create a new handler instance for this test
        SaslInitiationHandler handler2 = new SaslInitiationHandler();

        // First call
        handler.channelActive(context);

        // Second call with different handler instance (simulating concurrent connections)
        handler2.channelActive(context);

        // Verify both calls result in write attempts
        verify(context, times(2)).writeAndFlush(any(ByteBuf.class));
    }

    @Test
    void testChannelActiveWithNullRemoteAddress() {
        when(channel.remoteAddress()).thenReturn(null);

        handler.channelActive(context);

        // Should still attempt to send NUL byte
        verify(context).writeAndFlush(any(ByteBuf.class));
    }

    private void assertArrayEquals(byte[] expected, byte[] actual) {
        if (expected.length != actual.length) {
            throw new AssertionError(
                    "Array lengths differ: expected "
                            + expected.length
                            + " but was "
                            + actual.length);
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != actual[i]) {
                throw new AssertionError(
                        "Arrays differ at index "
                                + i
                                + ": expected "
                                + expected[i]
                                + " but was "
                                + actual[i]);
            }
        }
    }
}
