/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.impl;

import com.lucimber.dbus.connection.Handler;
import com.lucimber.dbus.connection.HandlerContext;
import com.lucimber.dbus.connection.Pipeline;
import com.lucimber.dbus.message.InboundMessage;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultHandlerContext implements HandlerContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Handler handler;
  private final String name;
  private final Pipeline pipeline;
  private DefaultHandlerContext previousContext;
  private DefaultHandlerContext nextContext;

  DefaultHandlerContext(final String name, final Pipeline pipeline, final Handler handler) {
    this.name = Objects.requireNonNull(name);
    this.pipeline = Objects.requireNonNull(pipeline);
    this.handler = Objects.requireNonNull(handler);
  }

  public DefaultHandlerContext getPreviousContext() {
    return previousContext;
  }

  public void setPreviousContext(final DefaultHandlerContext previousContext) {
    this.previousContext = previousContext;
  }

  public DefaultHandlerContext getNextContext() {
    return nextContext;
  }

  public void setNextContext(final DefaultHandlerContext nextContext) {
    this.nextContext = nextContext;
  }

  @Override
  public void passInboundMessage(final InboundMessage msg) {
    LoggerUtils.debug(LOGGER, () -> "Passing inbound message further up the pipeline.");
    final HandlerContext ctx = nextContext;
    try {
      ctx.getHandler().onInboundMessage(ctx, msg);
    } catch (Exception e) {
      ctx.getHandler().onInboundFailure(ctx, e);
    }
  }

  @Override
  public void passInboundFailure(final Throwable cause) {
    LoggerUtils.debug(LOGGER, () -> "Passing cause of inbound failure further up the pipeline.");
    final HandlerContext ctx = nextContext;
    ctx.getHandler().onInboundFailure(ctx, cause);
  }

  @Override
  public void passOutboundMessage(final OutboundMessage msg) {
    LoggerUtils.debug(LOGGER, () -> "Passing outbound message further down the pipeline.");
    final HandlerContext ctx = previousContext;
    try {
      ctx.getHandler().onOutboundMessage(ctx, msg);
    } catch (Exception ex) {
      ctx.passOutboundFailure(ex);
    }
  }

  @Override
  public void passOutboundFailure(final Throwable cause) {
    LoggerUtils.debug(LOGGER, () -> "Passing cause of outbound failure further up the pipeline.");
    final HandlerContext ctx = nextContext;
    ctx.getHandler().onOutboundFailure(ctx, cause);
  }

  @Override
  public Handler getHandler() {
    return handler;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Pipeline getPipeline() {
    return pipeline;
  }

  @Override
  public void passConnectionActiveEvent() {
    LoggerUtils.debug(LOGGER, () -> "Passing connection active event further up the pipeline.");
    final HandlerContext ctx = nextContext;
    ctx.getHandler().onConnectionActive(ctx);
  }

  @Override
  public void passConnectionInactiveEvent() {
    LoggerUtils.debug(LOGGER, () -> "Passing connection inactive event further up the pipeline.");
    final HandlerContext ctx = nextContext;
    ctx.getHandler().onConnectionInactive(ctx);
  }

  @Override
  public void passUserEvent(final Object event) {
    LoggerUtils.debug(LOGGER, () -> "Passing user event further up the pipeline.");
    final HandlerContext ctx = nextContext;
    ctx.getHandler().onUserEvent(ctx, event);
  }
}
