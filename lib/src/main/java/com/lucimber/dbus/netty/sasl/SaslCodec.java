/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import com.lucimber.dbus.netty.DBusChannelEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

@ChannelHandler.Sharable
public final class SaslCodec extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaslCodec.class);
  static final String SASL_MSG_DECODER_NAME = "saslMessageDecoder";
  static final String SASL_MSG_ENCODER_NAME = "saslMessageEncoder";

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
    ctx.pipeline().addBefore(ctx.name(), SASL_MSG_DECODER_NAME, new SaslMessageDecoder());
    ctx.pipeline().addBefore(ctx.name(), SASL_MSG_ENCODER_NAME, new SaslMessageEncoder());
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt == DBusChannelEvent.SASL_AUTH_COMPLETE) {
      handleSaslAuthCompleteEvent(ctx);
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  private void handleSaslAuthCompleteEvent(ChannelHandlerContext ctx) {
    ChannelPipeline pipeline = ctx.pipeline();

    for (var name : List.of(SASL_MSG_DECODER_NAME, SASL_MSG_ENCODER_NAME)) {
      if (pipeline.get(name) != null) {
        try {
          pipeline.remove(name);
          LOGGER.debug("Removed {} from pipeline.", name);
        } catch (NoSuchElementException ignored) {
        }
      }
    }

    try {
      pipeline.remove(this);
      LOGGER.debug("Removed myself as {} from pipeline.", SaslCodec.class.getSimpleName());
    } catch (NoSuchElementException ignored) {
      LOGGER.warn("Failed to remove myself as {} from pipeline.", SaslCodec.class.getSimpleName());
    }
  }
}
