/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.OutboundMessage;

import java.util.concurrent.CompletableFuture;

/**
 * A specialized {@link Handler} for processing outbound events in a {@link Pipeline}.
 * <p>
 * Outbound handlers process messages and events that flow from the tail toward the head
 * of the pipeline, typically in preparation for transmission.
 *
 * @see Context
 * @see Pipeline
 */
public interface OutboundHandler extends Handler {

  /**
   * Invoked when an outbound message is propagated through the pipeline.
   * <p>
   * This method is responsible for handling or forwarding the outbound {@link OutboundMessage}.
   * Completion of the provided {@link CompletableFuture} signals the success or failure of the operation.
   *
   * @param ctx    the {@link Context} this handler is bound to.
   * @param msg    the outbound message to process.
   * @param future the future to complete once the message has been handled or an error has occurred.
   */
  void handleOutboundMessage(Context ctx, OutboundMessage msg, CompletableFuture<Void> future);
}
