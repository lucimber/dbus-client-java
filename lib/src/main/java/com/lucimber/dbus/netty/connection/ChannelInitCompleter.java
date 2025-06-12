/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class ChannelInitCompleter extends ChannelInboundHandlerAdapter {

  private final CompletableFuture<Void> future;

  ChannelInitCompleter(final CompletableFuture<Void> future) {
    this.future = Objects.requireNonNull(future);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    if (evt == CustomChannelEvent.MANDATORY_NAME_ACQUIRED) {
      ctx.pipeline().remove(this);
      future.complete(null);
    } else {
      ctx.fireUserEventTriggered(evt);
    }
  }
}
