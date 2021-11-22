package com.lucimber.dbus.impl.netty.connection;

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
