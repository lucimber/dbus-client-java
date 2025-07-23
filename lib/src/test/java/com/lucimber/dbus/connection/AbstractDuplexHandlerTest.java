/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

class AbstractDuplexHandlerTest {

    private static class TestDuplexHandler extends AbstractDuplexHandler {
        @Override
        protected Logger getLogger() {
            return Mockito.mock(Logger.class);
        }
    }

    @Test
    void testDefaultBehavior() {
        var ctx = Mockito.mock(Context.class);
        var msgIn = Mockito.mock(InboundMessage.class);
        var msgOut = Mockito.mock(OutboundMessage.class);
        var future = new CompletableFuture<Void>();

        var handler = new TestDuplexHandler();

        handler.handleInboundFailure(ctx, new RuntimeException("fail"));
        handler.handleInboundMessage(ctx, msgIn);
        handler.handleOutboundMessage(ctx, msgOut, future);
        handler.handleUserEvent(ctx, "custom-event");
        handler.onConnectionActive(ctx);
        handler.onConnectionInactive(ctx);
        handler.onHandlerAdded(ctx);
        handler.onHandlerRemoved(ctx);

        Mockito.verify(ctx).propagateInboundFailure(Mockito.any());
        Mockito.verify(ctx).propagateInboundMessage(msgIn);
        Mockito.verify(ctx).propagateOutboundMessage(msgOut, future);
        Mockito.verify(ctx).propagateUserEvent("custom-event");
        Mockito.verify(ctx).propagateConnectionActive();
        Mockito.verify(ctx).propagateConnectionInactive();
    }
}
