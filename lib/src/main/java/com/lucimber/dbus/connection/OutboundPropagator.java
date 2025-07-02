/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.OutboundMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Defines a component that can forward outbound events through a {@link Pipeline}.
 * <p>
 * Typically implemented by {@link Context} instances or internal pipeline elements,
 * this interface supports propagating outbound messages from the tail toward the head.
 *
 * @see Context
 * @see OutboundMessage
 * @see Pipeline
 */
public interface OutboundPropagator {

  /**
   * Propagates an outbound message to the previous handler in the pipeline.
   * <p>
   * If not intercepted, the message will eventually reach the head handler.
   *
   * @param msg    the {@link OutboundMessage} to propagate.
   * @param future a {@link CompletableFuture} to complete once the message is successfully handled
   *               or if an error occurs during propagation.
   */
  void propagateOutboundMessage(OutboundMessage msg, CompletableFuture<Void> future);
}
