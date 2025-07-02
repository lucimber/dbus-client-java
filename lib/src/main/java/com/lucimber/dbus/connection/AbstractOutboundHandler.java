/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.OutboundMessage;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

/**
 * A skeletal implementation of the {@link OutboundHandler} interface.
 * <p>
 * This abstract base class provides default (no-op) implementations of all lifecycle methods
 * except for the core outbound handling method, which must be implemented by subclasses.
 * <p>
 * Extend this class to simplify the creation of handlers that only need to process outbound messages
 * and optionally react to connection or pipeline events.
 *
 * @see OutboundHandler
 * @see Context
 * @see Pipeline
 */
public abstract class AbstractOutboundHandler implements OutboundHandler {

  @Override
  public void handleOutboundMessage(Context ctx, OutboundMessage msg, CompletableFuture<Void> future) {
    getLogger().debug("Outbound message received. "
            + "No handler at this stage processed it. "
            + "Propagating toward the pipeline head.");
    ctx.propagateOutboundMessage(msg, future);
  }

  @Override
  public void handleUserEvent(Context ctx, Object evt) {
    getLogger().debug("User-defined event received. "
            + "No handler at this stage processed it. "
            + "Propagating toward the pipeline tail.");
    ctx.propagateUserEvent(evt);
  }

  @Override
  public void onConnectionActive(Context ctx) {
    getLogger().debug("Connection-active event received. "
            + "No handler at this stage processed it. "
            + "Propagating toward the pipeline tail.");
    ctx.propagateConnectionActive();
  }

  @Override
  public void onConnectionInactive(Context ctx) {
    getLogger().debug("Connection-inactive event received. "
            + "No handler at this stage processed it. "
            + "Propagating toward the pipeline tail.");
    ctx.propagateConnectionInactive();
  }

  @Override
  public void onHandlerAdded(Context ctx) {
    getLogger().debug("I have been added to a pipeline. Context: {}", ctx.getName());
  }

  @Override
  public void onHandlerRemoved(Context ctx) {
    getLogger().debug("I have been removed from a pipeline. Context: {}", ctx.getName());
  }

  /**
   * Returns the logger of the concrete class.
   *
   * @return the logger
   */
  abstract Logger getLogger();
}
