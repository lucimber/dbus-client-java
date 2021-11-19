package io.lucimber.dbus.impl.netty.connection;

import io.lucimber.dbus.connection.sasl.SaslAuthMechanism;
import io.lucimber.dbus.connection.sasl.SaslAuthMessage;
import io.lucimber.dbus.connection.sasl.SaslBeginMessage;
import io.lucimber.dbus.connection.sasl.SaslCommandName;
import io.lucimber.dbus.connection.sasl.SaslMessage;
import io.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static io.lucimber.dbus.connection.sasl.SaslCommandName.SERVER_OK;
import static io.lucimber.dbus.connection.sasl.SaslCommandName.SERVER_REJECTED;
import static io.lucimber.dbus.connection.sasl.SaslCommandName.SHARED_DATA;
import static io.lucimber.dbus.connection.sasl.SaslCommandName.SHARED_ERROR;

@ChannelHandler.Sharable
final class SaslAnonymousInboundHandler extends AbstractSaslInboundHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static ChannelFuture sendAuthMessage(final ChannelHandlerContext ctx) {
        LoggerUtils.debug(LOGGER, () -> "Sending auth message.");
        final String s = SaslAuthMechanism.ANONYMOUS.toString();
        final SaslMessage msg = new SaslAuthMessage(s);
        return ctx.writeAndFlush(msg);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        LoggerUtils.trace(LOGGER, () -> "Channel is now active.");
        final ChannelFuture future = sendAuthMessage(ctx);
        future.addListener(new DefaultFutureListener<>(ctx, LOGGER, v -> setCurrentState(State.WAITING_FOR_OK)));
    }

    @Override
    void handleMessageInDataState(final ChannelHandlerContext ctx, final SaslMessage msg) {
        LoggerUtils.trace(LOGGER, () -> "Handling incoming message state: " + getCurrentState());
        final SaslCommandName commandName = msg.getCommandName();
        if (commandName.equals(SHARED_ERROR)) {
            sendCancelMessage(ctx);
            setCurrentState(State.WAITING_FOR_REJECT);
        } else if (commandName.equals(SERVER_OK)) {
            handleOkMessage(ctx);
        } else {
            respondToWrongState(ctx);
        }
    }

    @Override
    void handleMessageInOkState(final ChannelHandlerContext ctx, final SaslMessage msg) {
        LoggerUtils.trace(LOGGER, () -> "Handling incoming message in state: " + getCurrentState());
        final SaslCommandName commandName = msg.getCommandName();
        if (commandName.equals(SERVER_OK)) {
            handleOkMessage(ctx);
        } else if (commandName.equals(SHARED_DATA)
                || commandName.equals(SHARED_ERROR)) {
            final ChannelFuture cancelFuture = sendCancelMessage(ctx);
            cancelFuture.addListener(new DefaultFutureListener<>(ctx, LOGGER,
                    v -> setCurrentState(State.WAITING_FOR_REJECT)));
        } else if (commandName.equals(SERVER_REJECTED)) {
            ctx.close();
        } else {
            respondToWrongState(ctx);
        }
    }

    private void handleOkMessage(final ChannelHandlerContext ctx) {
        LoggerUtils.debug(LOGGER, () -> "Sending begin message.");
        final ChannelFuture future = ctx.writeAndFlush(new SaslBeginMessage());
        future.addListener(new DefaultFutureListener<>(ctx, LOGGER, v -> {
            ctx.pipeline().remove(this);
            LoggerUtils.info(LOGGER, () -> "SASL authentication was completed successfully.");
            ctx.pipeline().fireUserEventTriggered(CustomChannelEvent.SASL_AUTH_COMPLETE);
        }));
    }
}
