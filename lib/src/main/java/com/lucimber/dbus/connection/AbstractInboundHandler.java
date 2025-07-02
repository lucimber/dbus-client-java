/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import org.slf4j.Logger;

/**
 * A skeletal implementation of the {@link InboundHandler} interface.
 * <p>
 * This abstract base class provides default (no-op) implementations of all lifecycle methods
 * except for the core inbound handling methods, which must be implemented by subclasses.
 * <p>
 * Extend this class to simplify the creation of handlers that only need to process inbound messages
 * and optionally handle connection or pipeline events.
 *
 * @see InboundHandler
 * @see Context
 * @see Pipeline
 */
public abstract class AbstractInboundHandler implements InboundHandler {

  @Override
  public void handleInboundFailure(Context ctx, Throwable cause) {
    getLogger().debug("Received a failure caused by an inbound message. "
            + "No action was taken. "
            + "Propagating the failure to the next component.");
    ctx.propagateInboundFailure(cause);
  }

  @Override
  public void handleInboundMessage(Context ctx, InboundMessage msg) {
    getLogger().debug("Inbound message received. "
            + "No handler at this stage processed it. "
            + "Propagating toward the pipeline head.");
    ctx.propagateInboundMessage(msg);
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
