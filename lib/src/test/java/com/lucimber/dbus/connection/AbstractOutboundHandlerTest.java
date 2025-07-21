/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.OutboundMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

class AbstractOutboundHandlerTest {

  private static class TestOutboundHandler extends AbstractOutboundHandler {

  @Override
  protected Logger getLogger() {
      return Mockito.mock(Logger.class);
  }
  }

  @Test
  void testDefaultOutboundPropagation() {
  var ctx = Mockito.mock(Context.class);
  var msg = Mockito.mock(OutboundMessage.class);
  var future = new CompletableFuture<Void>();
  var handler = new TestOutboundHandler();

  handler.handleOutboundMessage(ctx, msg, future);
  handler.handleUserEvent(ctx, "event");
  handler.onConnectionActive(ctx);
  handler.onConnectionInactive(ctx);
  handler.onHandlerAdded(ctx);
  handler.onHandlerRemoved(ctx);

  Mockito.verify(ctx).propagateOutboundMessage(msg, future);
  Mockito.verify(ctx).propagateUserEvent("event");
  Mockito.verify(ctx).propagateConnectionActive();
  Mockito.verify(ctx).propagateConnectionInactive();
  }
}
