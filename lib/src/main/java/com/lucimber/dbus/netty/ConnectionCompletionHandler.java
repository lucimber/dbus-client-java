/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper handler to complete the connection promise after full setup.
 */
class ConnectionCompletionHandler extends ChannelInboundHandlerAdapter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionCompletionHandler.class);
  private Promise<Void> connectPromise;

  public ConnectionCompletionHandler(Promise<Void> connectPromise) {
    this.connectPromise = connectPromise;
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt == DBusChannelEvent.MANDATORY_NAME_ACQUIRED) {
      if (connectPromise.trySuccess(null)) {
        LOGGER.debug("Connection process complete (mandatory name acquired). "
                + "Fulfilled connect promise.");
      }
      // Remove this handler from the pipeline as connection is complete
      ctx.pipeline().remove(this);
      LOGGER.debug("Removed ConnectionCompletionHandler from pipeline as connection is complete.");
    } else if (evt == DBusChannelEvent.SASL_AUTH_FAILED
            || evt == DBusChannelEvent.MANDATORY_NAME_ACQUISITION_FAILED) {
      String failureReason = "DBus connection setup failed: " + evt;
      if (connectPromise.tryFailure(new RuntimeException(failureReason))) {
        LOGGER.warn("Connection process failed at SASL or Mandatory Name stage. "
                + "Failed connect promise.");
      }
      // Remove this handler from the pipeline after failure
      ctx.pipeline().remove(this);
      LOGGER.debug("Removed ConnectionCompletionHandler from pipeline after failure.");
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    if (connectPromise.tryFailure(new java.nio.channels.ClosedChannelException())) {
      LOGGER.warn("Channel became inactive before connection process completed. "
              + "Failing connect promise.");
    }
    super.channelInactive(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (connectPromise.tryFailure(cause)) {
      LOGGER.error("Exception during connection setup. Failing connect promise.", cause);
    }
    // Don't remove self here, let the pipeline manage error propagation
    // The channel will likely be closed by a tail handler or by Sasl/MandatoryName handlers.
    super.exceptionCaught(ctx, cause);
  }

  /**
   * Resets the connection completion handler to its initial state for reconnection.
   * This method is called when the connection needs to be re-established.
   * 
   * @param newConnectPromise the new promise to use for the reconnection attempt
   */
  public void reset(Promise<Void> newConnectPromise) {
    LOGGER.debug("Resetting ConnectionCompletionHandler for reconnection");
    this.connectPromise = newConnectPromise;
  }
}
