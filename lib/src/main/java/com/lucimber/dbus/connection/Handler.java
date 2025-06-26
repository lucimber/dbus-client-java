/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;

/**
 * A handler is assigned to a pipeline and reacts to events that arise within this pipeline.
 *
 * @see Pipeline
 * @see HandlerContext
 */
public interface Handler {

  /**
   * Processes the activation of the D-Bus connection
   * or forwards it to the next receiver on the {@link Pipeline}.
   * Gets called after the connection was established and the SASL authentication was successful
   * and the mandatory D-Bus name has been acquired.
   *
   * @param ctx the {@link HandlerContext} of this {@link Handler}
   */
  default void onConnectionActive(HandlerContext ctx) {
    ctx.passConnectionActiveEvent();
  }

  /**
   * Processes the inactivation of the D-Bus connection
   * or forwards it to the next receiver on the {@link Pipeline}.
   * Gets called after the connection was closed.
   *
   * @param ctx the {@link HandlerContext} of this {@link Handler}
   */
  default void onConnectionInactive(HandlerContext ctx) {
    ctx.passConnectionInactiveEvent();
  }

  /**
   * Processes an inbound message or propagates the message further up the pipeline,
   * eventually reaching the last handler.
   *
   * @param ctx the {@link HandlerContext} of this {@link Handler}
   * @param msg the {@link InboundMessage}
   * @throws Exception If an inbound message could not be processed correctly.
   */
  default void onInboundMessage(HandlerContext ctx, InboundMessage msg) throws Exception {
    ctx.passInboundMessage(msg);
  }

  /**
   * Processes an outbound message or propagates the message further down the pipeline,
   * eventually reaching the first handler.
   *
   * @param ctx the {@link HandlerContext} of this {@link Handler}
   * @param msg the {@link OutboundMessage}
   * @throws Exception If an outbound message could not be processed correctly.
   */
  default void onOutboundMessage(HandlerContext ctx, OutboundMessage msg) throws Exception {
    ctx.passOutboundMessage(msg);
  }

  /**
   * Processes the user event or forwards it to the next receiver on the {@link Pipeline}.
   *
   * @param ctx   the {@link HandlerContext} of this {@link Handler}
   * @param event the user event
   */
  default void onUserEvent(HandlerContext ctx, Object event) {
    ctx.passUserEvent(event);
  }

  /**
   * Processes the cause of an inbound failure or propagates the cause further up the pipeline,
   * eventually reaching the last handler.
   *
   * @param ctx   the {@link HandlerContext} of this {@link Handler}
   * @param cause the cause of the failure
   */
  default void onInboundFailure(HandlerContext ctx, Throwable cause) {
    ctx.passInboundFailure(cause);
  }

  /**
   * Processes the cause of an outbound failure or propagates the cause further up the pipeline,
   * eventually reaching the last handler.
   *
   * @param ctx   the {@link HandlerContext} of this {@link Handler}
   * @param cause the cause of the failure
   */
  default void onOutboundFailure(HandlerContext ctx, Throwable cause) {
    ctx.passOutboundFailure(cause);
  }
}
