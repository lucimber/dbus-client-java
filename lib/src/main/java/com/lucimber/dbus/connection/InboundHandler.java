/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import com.lucimber.dbus.message.InboundMessage;

/**
 * A specialized {@link Handler} for processing inbound events in a {@link Pipeline}.
 *
 * <p>Inbound handlers are responsible for handling messages and failures as they travel
 * from the head to the tail of the pipeline.
 *
 * @see Context
 * @see Pipeline
 */
public interface InboundHandler extends Handler {

  /**
   * Invoked when an error occurs during the processing of an inbound message.
   *
   * <p>This method can be used to log, transform, or recover from the error as appropriate.
   *
   * @param ctx   the {@link Context} this handler is bound to.
   * @param cause the {@link Throwable} describing the failure.
   */
  void handleInboundFailure(Context ctx, Throwable cause);

  /**
   * Invoked when an inbound message is received and propagated through the pipeline.
   *
   * <p>Handlers may inspect, transform, or act upon the message before forwarding it
   * downstream.
   *
   * @param ctx the {@link Context} this handler is bound to.
   * @param msg the {@link InboundMessage} being processed.
   */
  void handleInboundMessage(Context ctx, InboundMessage msg);
}
