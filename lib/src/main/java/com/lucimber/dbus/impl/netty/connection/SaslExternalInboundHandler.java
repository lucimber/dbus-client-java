package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.sasl.SaslAuthMechanism;
import com.lucimber.dbus.connection.sasl.SaslAuthMessage;
import com.lucimber.dbus.connection.sasl.SaslBeginMessage;
import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.util.LoggerUtils;
import com.lucimber.dbus.util.SaslUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SaslExternalInboundHandler extends AbstractSaslInboundHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final String identity;

  SaslExternalInboundHandler(final String identity) {
    Objects.requireNonNull(identity);
    this.identity = SaslUtils.toHexadecimalString(identity.getBytes(StandardCharsets.US_ASCII));
  }

  private void sendAuthMessageAndChangeState(final ChannelHandlerContext ctx) {
    final ChannelFuture future = sendAuthMessage(ctx);
    future.addListener(new DefaultFutureListener<>(LOGGER, v -> setCurrentState(State.WAITING_FOR_OK)));
  }

  private ChannelFuture sendAuthMessage(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "Sending auth message.");
    final String s = String.format("%s %s", SaslAuthMechanism.EXTERNAL, identity);
    final SaslMessage msg = new SaslAuthMessage(s);
    return ctx.writeAndFlush(msg);
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "I have been added to a channel pipeline.");
    sendAuthMessageAndChangeState(ctx);
  }

  @Override
  void handleMessageInDataState(final ChannelHandlerContext ctx, final SaslMessage msg) {
    LoggerUtils.trace(LOGGER, () -> "Handling incoming message in state: " + State.WAITING_FOR_DATA);
    sendCancelMessage(ctx)
            .addListener(new DefaultFutureListener<>(LOGGER, v -> setCurrentState(State.WAITING_FOR_REJECT)));
  }

  @Override
  void handleMessageInOkState(final ChannelHandlerContext ctx, final SaslMessage msg) {
    LoggerUtils.trace(LOGGER, () -> "Handling incoming message in state: " + State.WAITING_FOR_OK);
    final SaslCommandName commandName = msg.getCommandName();
    if (commandName.equals(SaslCommandName.SERVER_OK)) {
      handleOkMessage(ctx);
    } else if (commandName.equals(SaslCommandName.SHARED_DATA)
            || commandName.equals(SaslCommandName.SHARED_ERROR)) {
      sendCancelMessage(ctx);
      setCurrentState(State.WAITING_FOR_REJECT);
    } else if (commandName.equals(SaslCommandName.SERVER_REJECTED)) {
      ctx.close();
    } else {
      respondToWrongState(ctx);
    }
  }

  private void handleOkMessage(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "Sending begin message.");
    final ChannelFuture future = ctx.writeAndFlush(new SaslBeginMessage());
    future.addListener(new DefaultFutureListener<>(LOGGER, v -> {
      LoggerUtils.trace(LOGGER, () -> "Detaching from channel pipeline.");
      ctx.pipeline().remove(this);
      LoggerUtils.info(LOGGER, () -> "SASL authentication was completed successfully.");
      ctx.pipeline().fireUserEventTriggered(CustomChannelEvent.SASL_AUTH_COMPLETE);
    }));
  }
}
