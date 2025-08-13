/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class DefaultPipelineTest {

    private Connection mockConnection;
    private DefaultPipeline pipeline;

    @BeforeEach
    void setUp() {
        mockConnection = Mockito.mock(Connection.class);
        pipeline = new DefaultPipeline(mockConnection);
    }

    @Test
    void testAddAndRemoveHandler() {
        Handler handler = Mockito.mock(Handler.class);
        pipeline.addLast("testHandler", handler);
        pipeline.remove("testHandler");

        // Handler lifecycle methods should be called
        Mockito.verify(handler).onHandlerAdded(Mockito.any());
        Mockito.verify(handler).onHandlerRemoved(Mockito.any());
    }

    @Test
    void testAddDuplicateHandlerFails() {
        Handler handler1 = Mockito.mock(Handler.class);
        Handler handler2 = Mockito.mock(Handler.class);
        pipeline.addLast("duplicateHandler", handler1);

        assertThrows(
                IllegalArgumentException.class,
                () -> pipeline.addLast("duplicateHandler", handler2));
    }

    @Test
    void testRemoveNonExistentHandlerFails() {
        assertThrows(IllegalArgumentException.class, () -> pipeline.remove("notFound"));
    }

    @Test
    void testCannotRemoveHeadOrTail() {
        assertThrows(IllegalArgumentException.class, () -> pipeline.remove("HEAD"));
        assertThrows(IllegalArgumentException.class, () -> pipeline.remove("TAIL"));
    }

    @Test
    void testPropagateInboundMessage() {
        InboundHandler handler = Mockito.mock(InboundHandler.class);
        pipeline.addLast("inbound", handler);

        InboundMessage msg = Mockito.mock(InboundMessage.class);
        pipeline.propagateInboundMessage(msg);

        Mockito.verify(handler).handleInboundMessage(Mockito.any(), Mockito.eq(msg));
    }

    @Test
    void testPropagateOutboundMessage() {
        OutboundHandler handler = Mockito.mock(OutboundHandler.class);
        pipeline.addLast("outbound", handler);

        OutboundMessage msg = Mockito.mock(OutboundMessage.class);
        CompletableFuture<Void> future = new CompletableFuture<>();
        pipeline.propagateOutboundMessage(msg, future);

        Mockito.verify(handler)
                .handleOutboundMessage(Mockito.any(), Mockito.eq(msg), Mockito.eq(future));
    }

    @Test
    void testPropagateInboundFailure() {
        InboundHandler handler = Mockito.mock(InboundHandler.class);
        pipeline.addLast("failure", handler);

        Throwable cause = new RuntimeException("Simulated error");
        pipeline.propagateInboundFailure(cause);

        Mockito.verify(handler).handleInboundFailure(Mockito.any(), Mockito.eq(cause));
    }

    @Test
    void testPropagateConnectionEvents() {
        Handler handler = Mockito.mock(Handler.class);
        pipeline.addLast("events", handler);

        pipeline.propagateConnectionActive();
        pipeline.propagateConnectionInactive();

        InOrder inOrder = Mockito.inOrder(handler);
        inOrder.verify(handler).onConnectionActive(Mockito.any());
        inOrder.verify(handler).onConnectionInactive(Mockito.any());
    }

    @Test
    void testGetConnection() {
        assertSame(mockConnection, pipeline.getConnection());
    }
}
