/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.sasl.SaslAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslAuthMechanism;
import com.lucimber.dbus.connection.sasl.SaslAuthMessage;
import com.lucimber.dbus.connection.sasl.SaslCancelMessage;
import com.lucimber.dbus.connection.sasl.SaslCommandName;
import com.lucimber.dbus.connection.sasl.SaslCookieAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslErrorMessage;
import com.lucimber.dbus.connection.sasl.SaslExternalAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslMessage;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SaslMechanismInboundHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final LinkedHashMap<SaslAuthMechanism, SaslAuthConfig> authSuites;
  private boolean serverPreviouslyAnsweredWithoutList = false;

  SaslMechanismInboundHandler(final LinkedHashMap<SaslAuthMechanism, SaslAuthConfig> authSuites) {
    Objects.requireNonNull(authSuites);
    if (authSuites.isEmpty()) {
      throw new IllegalArgumentException("map must not be empty");
    }
    this.authSuites = new LinkedHashMap<>(authSuites);
  }

  private static ChannelFuture requestAvailableAuthMechanisms(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "Requesting available authentication mechanisms.");
    final SaslMessage msg = new SaslAuthMessage();
    final ChannelPromise promise = ctx.newPromise();
    return ctx.writeAndFlush(msg, promise);
  }

  private static void sendCancelMessageAndCloseChannel(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "Sending cancel message and closing channel.");
    final SaslMessage msg = new SaslCancelMessage();
    final ChannelFuture future = ctx.writeAndFlush(msg);
    future.addListener(new DefaultFutureListener<>(LOGGER, v -> ctx.close()));
  }

  private static ChannelFuture sendErrorMessage(final ChannelHandlerContext ctx) {
    LoggerUtils.debug(LOGGER, () -> "Sending error message.");
    final String error = "Missing mechanism list";
    final SaslMessage msg = new SaslErrorMessage(error);
    return ctx.writeAndFlush(msg);
  }

  private static void replaceWithAnonymousHandler(final ChannelHandlerContext ctx, final String oldHandlerName) {
    final String newHandlerName = SaslAnonymousInboundHandler.class.getSimpleName();
    final ChannelInboundHandler handler = new SaslAnonymousInboundHandler();
    LoggerUtils.debug(LOGGER, () -> String.format("Replacing myself with %s in channel pipeline", newHandlerName));
    ctx.pipeline().replace(oldHandlerName, newHandlerName, handler);
  }

  private static void replaceWithCookieHandler(final ChannelHandlerContext ctx, final String oldHandlerName,
                                               final SaslCookieAuthConfig authConfig) {
    final String newHandlerName = SaslCookieInboundHandler.class.getSimpleName();
    final String identity = authConfig.getIdentity();
    final String absCookieDirPath = authConfig.getAbsCookieDirPath();
    final ChannelInboundHandler handler = new SaslCookieInboundHandler(identity, absCookieDirPath);
    LoggerUtils.debug(LOGGER, () -> String.format("Replacing myself with %s in channel pipeline", newHandlerName));
    ctx.pipeline().replace(oldHandlerName, newHandlerName, handler);
  }

  private static void replaceWithExternalHandler(final ChannelHandlerContext ctx, final String oldHandlerName,
                                                 final SaslExternalAuthConfig authConfig) {
    final String newHandlerName = SaslExternalInboundHandler.class.getSimpleName();
    final String identity = authConfig.getIdentity();
    final ChannelInboundHandler handler = new SaslExternalInboundHandler(identity);
    LoggerUtils.debug(LOGGER, () -> String.format("Replacing myself with %s in channel pipeline", newHandlerName));
    ctx.pipeline().replace(oldHandlerName, newHandlerName, handler);
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    LoggerUtils.debug(LOGGER, () -> "Received new message from peer.");
    if (msg instanceof SaslMessage) {
      handleServerResponse(ctx, (SaslMessage) msg);
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    LoggerUtils.error(LOGGER, () -> "Received a throwable.", cause);
    LoggerUtils.debug(LOGGER, () -> "Ignoring and passing caught exception to next inbound handler.");
    ctx.fireExceptionCaught(cause);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    LoggerUtils.debug(LOGGER, () -> "Received user event: " + evt);
    if (evt == CustomChannelEvent.SASL_NUL_BYTE_SENT) {
      final ChannelFuture future = requestAvailableAuthMechanisms(ctx);
      future.addListener(new DefaultFutureListener<>(LOGGER, v -> ctx.fireUserEventTriggered(evt)));
    }
    ctx.fireUserEventTriggered(evt);
  }

  private SaslAuthMechanism selectMechanism(final String[] peerMechanisms) {
    LoggerUtils.trace(LOGGER, () -> "Selecting authentication mechanism.");
    for (SaslAuthMechanism providedMechanism : authSuites.keySet()) {
      for (String mechanism : peerMechanisms) {
        if (providedMechanism.toString().equals(mechanism)) {
          return providedMechanism;
        }
      }
    }
    return null;
  }

  private void handleServerResponse(final ChannelHandlerContext ctx, final SaslMessage msg) {
    if (msg.getCommandName().equals(SaslCommandName.SERVER_REJECTED)) {
      LoggerUtils.debug(LOGGER, () -> "Peer answered with REJECTED command.");
      if (msg.getCommandValue().isPresent()) {
        final String[] mechanisms = msg.getCommandValue().get().split("\\s");
        final SaslAuthMechanism selectedMechanism = selectMechanism(mechanisms);
        if (selectedMechanism == null) {
          LoggerUtils.debug(LOGGER, () -> "No auth mechanism has been selected.");
          sendCancelMessageAndCloseChannel(ctx);
        } else {
          LoggerUtils.debug(LOGGER, () -> "Selected mechanism: " + selectedMechanism);
          final String baseName = SaslMechanismInboundHandler.class.getSimpleName();
          if (selectedMechanism.equals(SaslAuthMechanism.EXTERNAL)) {
            final SaslAuthConfig authConfig = authSuites.get(SaslAuthMechanism.EXTERNAL);
            if (authConfig == null) {
              LoggerUtils.error(LOGGER, () -> "No config provided for external authentication.");
              sendCancelMessageAndCloseChannel(ctx);
            }
            if (authConfig instanceof SaslExternalAuthConfig) {
              replaceWithExternalHandler(ctx, baseName, (SaslExternalAuthConfig) authConfig);
            } else {
              LoggerUtils.error(LOGGER, () -> "Could not use config for external authentication.");
              sendCancelMessageAndCloseChannel(ctx);
            }
          } else if (selectedMechanism.equals(SaslAuthMechanism.COOKIE)) {
            final SaslAuthConfig authConfig = authSuites.get(SaslAuthMechanism.COOKIE);
            if (authConfig == null) {
              LoggerUtils.error(LOGGER, () -> "No config provided for authentication via cookie.");
              sendCancelMessageAndCloseChannel(ctx);
            }
            if (authConfig instanceof SaslCookieAuthConfig) {
              replaceWithCookieHandler(ctx, baseName, (SaslCookieAuthConfig) authConfig);
            } else {
              LoggerUtils.error(LOGGER, () -> "Could not use config for authentication via cookie.");
              sendCancelMessageAndCloseChannel(ctx);
            }
          } else {
            replaceWithAnonymousHandler(ctx, baseName);
          }
        }
      } else {
        LoggerUtils.debug(LOGGER, () -> "Peer answered with unexpected command: " + msg);
        if (serverPreviouslyAnsweredWithoutList) {
          sendCancelMessageAndCloseChannel(ctx);
        } else {
          serverPreviouslyAnsweredWithoutList = true;
          sendErrorMessage(ctx).addListener(new DefaultFutureListener<>(LOGGER));
        }
      }
    }
  }
}
