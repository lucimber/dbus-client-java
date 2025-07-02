/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.OutboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

final class InternalHeadHandler extends AbstractDuplexHandler implements InboundHandler, OutboundHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalHeadHandler.class);

  @Override
  public void handleOutboundMessage(Context ctx, OutboundMessage msg, CompletableFuture<Void> future) {
    ctx.getConnection().sendAndRouteResponse(msg, future);
  }

  @Override
  Logger getLogger() {
    return LOGGER;
  }
}
