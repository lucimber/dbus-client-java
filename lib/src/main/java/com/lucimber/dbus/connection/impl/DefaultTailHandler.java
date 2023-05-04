/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.impl;

import com.lucimber.dbus.connection.Handler;
import com.lucimber.dbus.connection.HandlerContext;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultTailHandler implements Handler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void onConnectionActive(final HandlerContext ctx) {
    LoggerUtils.info(LOGGER, () -> "Discarding connection active event that reached the end of the pipeline.");
  }

  @Override
  public void onConnectionInactive(final HandlerContext ctx) {
    LoggerUtils.info(LOGGER, () -> "Discarding connection inactive event that reached the end of the pipeline.");
  }

  @Override
  public void onInboundMessage(final HandlerContext ctx, final InboundMessage msg) {
    LoggerUtils.warn(LOGGER, () -> {
      final String format = "Discarding an inbound message that reached the end of the pipeline: %s";
      return String.format(format, msg);
    });
  }

  @Override
  public void onUserEvent(final HandlerContext ctx, final Object event) {
    LoggerUtils.warn(LOGGER, () -> {
      final String format = "Discarding an user event that reached the end of the pipeline: %s";
      return String.format(format, event);
    });
  }

  @Override
  public void onInboundFailure(final HandlerContext ctx, final Throwable cause) {
    LoggerUtils.warn(LOGGER, () -> {
      final String format = "Discarding cause of an inbound failure that reached the end of the pipeline: %s";
      return String.format(format, cause);
    });
  }

  @Override
  public void onOutboundFailure(final HandlerContext ctx, final Throwable cause) {
    LoggerUtils.warn(LOGGER, () -> {
      final String format = "Discarding cause of an outbound failure that reached the end of the pipeline: %s";
      return String.format(format, cause);
    });
  }

  @Override
  public void onOutboundMessage(final HandlerContext ctx, final OutboundMessage msg) {
    ctx.passOutboundMessage(msg);
  }
}
