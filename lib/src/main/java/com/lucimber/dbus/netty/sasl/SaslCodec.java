/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.netty.DBusChannelEvent;
import com.lucimber.dbus.netty.DBusHandlerNames;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SaslCodec extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaslCodec.class);

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    LOGGER.debug(LoggerUtils.HANDLER_LIFECYCLE, "Added using context name '{}'.", ctx.name());

    try {
      ctx.pipeline().addBefore(ctx.name(), DBusHandlerNames.SASL_MESSAGE_DECODER, new SaslMessageDecoder());
      ctx.pipeline().addBefore(ctx.name(), DBusHandlerNames.SASL_MESSAGE_ENCODER, new SaslMessageEncoder());
    } catch (RuntimeException e) {
      LOGGER.error(LoggerUtils.HANDLER_LIFECYCLE, "Failed to add handlers.", e);
      throw e;
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
    LOGGER.debug(LoggerUtils.HANDLER_LIFECYCLE, "Removed using context name '{}'.", ctx.name());

    try {
      ctx.pipeline().remove(DBusHandlerNames.SASL_MESSAGE_DECODER);
      ctx.pipeline().remove(DBusHandlerNames.SASL_MESSAGE_ENCODER);
    } catch (NoSuchElementException e) {
      LOGGER.warn(LoggerUtils.HANDLER_LIFECYCLE, "Failed to remove handlers.", e);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt == DBusChannelEvent.SASL_AUTH_COMPLETE) {
      handleSaslAuthCompleteEvent(ctx);
      ctx.fireUserEventTriggered(evt);
    } else {
      ctx.fireUserEventTriggered(evt);
    }
  }

  private void handleSaslAuthCompleteEvent(ChannelHandlerContext ctx) {
    // Remove this handler from the pipeline as SASL phase is complete
    ctx.pipeline().remove(this);
    LOGGER.debug(LoggerUtils.HANDLER_LIFECYCLE,
            "Removed SASL codec from pipeline as SASL authentication is complete.");
  }
}
