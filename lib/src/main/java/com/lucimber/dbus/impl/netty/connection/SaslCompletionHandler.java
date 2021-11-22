package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * The SASL procedure needs to be completed before any D-Bus related messages are being exchanged.
 * Therefore this handler captures the outcome of the SASL procedure and completes,
 * either successful or exceptionally the {@link java.util.concurrent.CompletionStage} of the
 * D-Bus {@link Connection}.
 *
 * @see <a href="https://dbus.freedesktop.org/doc/dbus-specification.html#auth-protocol"
 * target="_top">Desktop Bus Specification (Authentication Protocol)</a>
 */
final class SaslCompletionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
        LoggerUtils.trace(LOGGER, () -> String.format("Received user event: %s", evt));
        if (evt == CustomChannelEvent.SASL_AUTH_COMPLETE) {
            removeSaslRelatedOutboundHandlers(ctx.pipeline());
            ctx.pipeline().remove(this);
        }
        ctx.fireUserEventTriggered(evt);
    }

    private void removeSaslRelatedOutboundHandlers(final ChannelPipeline pipeline) {
        LoggerUtils.trace(LOGGER, () -> "Removing SASL related outbound handlers from channel pipeline.");
        pipeline.remove(SaslMessageEncoder.class.getSimpleName());
        pipeline.remove(SaslStringEncoder.class.getSimpleName());
        LoggerUtils.trace(LOGGER, pipeline::toString);
    }
}
