/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

/**
 * An abstract base class that combines both {@link InboundHandler} and {@link OutboundHandler}
 * interfaces to handle bidirectional message flow in a {@link Pipeline}.
 *
 * <p>Subclasses can override one or both sets of handler methods to implement custom logic
 * for inbound and outbound processing. This class is useful for handlers that need to
 * observe or manipulate both directions of message flow.
 *
 * @see InboundHandler
 * @see OutboundHandler
 * @see Pipeline
 * @see Context
 */
public abstract class AbstractDuplexHandler implements InboundHandler, OutboundHandler {

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
            + "Propagating toward the pipeline tail.");
    ctx.propagateInboundMessage(msg);
  }

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
   * Returns the logger of the subclass.
   *
   * @return the logger
   */
  protected abstract Logger getLogger();
}
