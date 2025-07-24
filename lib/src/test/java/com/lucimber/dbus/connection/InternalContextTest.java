/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lucimber.dbus.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InternalContextTest {

    private InternalContext context;
    private InternalContext nextContext;
    private InternalContext prevContext;
    private InboundHandler inboundHandler;
    private OutboundHandler outboundHandler;
    private Connection mockConnection;
    private DefaultPipeline pipeline;

    @BeforeEach
    void setUp() {
        mockConnection = Mockito.mock(Connection.class);
        pipeline = new DefaultPipeline(mockConnection);

        inboundHandler = Mockito.mock(InboundHandler.class);
        outboundHandler = Mockito.mock(OutboundHandler.class);

        context = new InternalContext(mockConnection, pipeline, "test", inboundHandler);
        nextContext = Mockito.mock(InternalContext.class);
        prevContext = Mockito.mock(InternalContext.class);

        context.setNext(nextContext);
        context.setPrev(prevContext);
    }

    @Test
    void testPropagateInboundMessage() {
        InboundMessage msg = Mockito.mock(InboundMessage.class);
        context.propagateInboundMessage(msg);
        Mockito.verify(nextContext).handleInboundMessage(msg);
    }

    @Test
    void testPropagateInboundFailure() {
        Throwable cause = new RuntimeException("Failure");
        context.propagateInboundFailure(cause);
        Mockito.verify(nextContext).handleInboundFailure(cause);
    }

    @Test
    void testPropagateOutboundMessage() {
        OutboundMessage msg = Mockito.mock(OutboundMessage.class);
        CompletableFuture<Void> future = new CompletableFuture<>();
        context.propagateOutboundMessage(msg, future);
        Mockito.verify(prevContext).handleOutboundMessage(msg, future);
    }

    @Test
    void testPropagateConnectionEvents() {
        context.propagateConnectionActive();
        context.propagateConnectionInactive();
        Mockito.verify(nextContext).onConnectionActive();
        Mockito.verify(nextContext).onConnectionInactive();
    }

    @Test
    void testHandleInboundDelegatesToHandler() {
        InboundMessage msg = Mockito.mock(InboundMessage.class);
        context.handleInboundMessage(msg);
        Mockito.verify(inboundHandler).handleInboundMessage(context, msg);
    }

    @Test
    void testHandleOutboundDelegatesToHandler() {
        InternalContext outboundCtx =
                new InternalContext(mockConnection, pipeline, "out", outboundHandler);
        OutboundMessage msg = Mockito.mock(OutboundMessage.class);
        CompletableFuture<Void> future = new CompletableFuture<>();
        outboundCtx.handleOutboundMessage(msg, future);
        Mockito.verify(outboundHandler).handleOutboundMessage(outboundCtx, msg, future);
    }

    @Test
    void testHandleUserEventInvokesHandler() {
        Handler genericHandler = Mockito.mock(Handler.class);
        InternalContext eventCtx =
                new InternalContext(mockConnection, pipeline, "evt", genericHandler);
        eventCtx.handleUserEvent("evt1");
        Mockito.verify(genericHandler).handleUserEvent(eventCtx, "evt1");
    }

    @Test
    void testConnectionAndPipelineAccessors() {
        assertEquals(mockConnection, context.getConnection());
        assertEquals(pipeline, context.getPipeline());
        assertEquals("test", context.getName());
        assertEquals(inboundHandler, context.getHandler());
    }

    @Test
    void testSettersAndGetters() {
        assertEquals(nextContext, context.getNext());
        assertEquals(prevContext, context.getPrev());
    }
}
