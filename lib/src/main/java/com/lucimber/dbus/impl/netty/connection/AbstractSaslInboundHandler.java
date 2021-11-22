package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.sasl.SaslCancelMessage;
import com.lucimber.dbus.connection.sasl.SaslErrorMessage;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.lang.invoke.MethodHandles;

abstract class AbstractSaslInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Marker MARKER = MarkerFactory.getMarker(LoggerUtils.MARKER_SASL_INBOUND);

    private State currentState;

    static ChannelFuture sendCancelMessage(final ChannelHandlerContext ctx) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Sending cancel message.");
        return ctx.writeAndFlush(new SaslCancelMessage());
    }

    static ChannelFuture sendErrorMessage(final ChannelHandlerContext ctx, final String error) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Sending error message.");
        final SaslMessage msg = new SaslErrorMessage(error);
        return ctx.writeAndFlush(msg);
    }

    static void respondToWrongState(final ChannelHandlerContext ctx) {
        LoggerUtils.debug(LOGGER, MARKER, () -> "Responding to wrong state.");
        final String error = "Wrong state";
        sendErrorMessage(ctx, error);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Received new message: " + msg);
        if (msg instanceof SaslMessage) {
            final SaslMessage saslMessage = (SaslMessage) msg;
            LoggerUtils.debug(LOGGER, MARKER, () -> "Handling received message: " + saslMessage);
            handleMessage(ctx, saslMessage);
        } else {
            LoggerUtils.debug(LOGGER, MARKER,
                    () -> "Ignoring received message and handing it over to next channel handler.");
            ctx.fireChannelRead(msg);
        }
    }

    private void handleMessage(final ChannelHandlerContext ctx, final SaslMessage msg) {
        if (State.WAITING_FOR_DATA.equals(currentState)) {
            handleMessageInDataState(ctx, msg);
        } else if (State.WAITING_FOR_OK.equals(currentState)) {
            handleMessageInOkState(ctx, msg);
        } else if (State.WAITING_FOR_REJECT.equals(currentState)) {
            ctx.close();
        } else {
            respondToWrongState(ctx);
            currentState = State.WAITING_FOR_REJECT;
        }
    }

    void setCurrentState(final State currentState) {
        LoggerUtils.trace(LOGGER, MARKER, () -> "Changing state to " + currentState.name() + ".");
        this.currentState = currentState;
    }

    State getCurrentState() {
        return currentState;
    }

    abstract void handleMessageInDataState(ChannelHandlerContext ctx, SaslMessage msg);

    abstract void handleMessageInOkState(ChannelHandlerContext ctx, SaslMessage msg);

    enum State {
        WAITING_FOR_DATA, WAITING_FOR_OK, WAITING_FOR_REJECT
    }
}
