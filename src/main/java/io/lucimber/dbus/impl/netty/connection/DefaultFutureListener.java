package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Consumer;

final class DefaultFutureListener<T extends Future<?>> implements GenericFutureListener<T> {

    private final ChannelHandlerContext ctx;
    private final Consumer<T> futureConsumer;
    private final Logger logger;

    DefaultFutureListener(final ChannelHandlerContext ctx, final Logger logger) {
        this(ctx, logger, null);
    }

    DefaultFutureListener(final ChannelHandlerContext ctx, final Logger logger, final Consumer<T> futureConsumer) {
        this.ctx = Objects.requireNonNull(ctx);
        this.logger = Objects.requireNonNull(logger);
        this.futureConsumer = futureConsumer;
    }

    @Override
    public void operationComplete(final T future) {
        if (future.isSuccess()) {
            LoggerUtils.debug(logger, () -> "I/O operation was completed successfully.");
        } else if (future.cause() != null) {
            LoggerUtils.error(logger, () -> "I/O operation was completed with failure.", future.cause());
            ctx.fireExceptionCaught(future.cause());
        } else if (future.isCancelled()) {
            LoggerUtils.debug(logger, () -> "I/O operation was completed by cancellation.");
        }
        if (futureConsumer != null) {
            futureConsumer.accept(future);
        }
    }
}
