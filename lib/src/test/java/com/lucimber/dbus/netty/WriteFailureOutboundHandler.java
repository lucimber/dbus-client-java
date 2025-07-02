/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.Objects;

final class WriteFailureOutboundHandler extends ChannelOutboundHandlerAdapter {

  private final Throwable cause;

  WriteFailureOutboundHandler(final Throwable cause) {
    this.cause = Objects.requireNonNull(cause);
  }

  @Override
  public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
    promise.tryFailure(cause);
  }
}
