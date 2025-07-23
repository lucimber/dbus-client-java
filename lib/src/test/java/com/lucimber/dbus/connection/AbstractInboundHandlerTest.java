/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

class AbstractInboundHandlerTest {

    private static class TestInboundHandler extends AbstractInboundHandler {

        @Override
        protected Logger getLogger() {
            return Mockito.mock(Logger.class);
        }
    }

    @Test
    void testDefaultInboundPropagation() {
        var ctx = Mockito.mock(Context.class);
        var msg = Mockito.mock(InboundMessage.class);
        var handler = new TestInboundHandler();

        handler.handleInboundFailure(ctx, new RuntimeException("fail"));
        handler.handleInboundMessage(ctx, msg);
        handler.handleUserEvent(ctx, "event");
        handler.onConnectionActive(ctx);
        handler.onConnectionInactive(ctx);
        handler.onHandlerAdded(ctx);
        handler.onHandlerRemoved(ctx);

        Mockito.verify(ctx).propagateInboundFailure(Mockito.any());
        Mockito.verify(ctx).propagateInboundMessage(msg);
        Mockito.verify(ctx).propagateUserEvent("event");
        Mockito.verify(ctx).propagateConnectionActive();
        Mockito.verify(ctx).propagateConnectionInactive();
    }
}
