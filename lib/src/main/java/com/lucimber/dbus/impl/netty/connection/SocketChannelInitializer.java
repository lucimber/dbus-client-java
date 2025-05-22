/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.impl.netty.connection;

import com.lucimber.dbus.connection.sasl.SaslAnonymousAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslAuthMechanism;
import com.lucimber.dbus.connection.sasl.SaslCookieAuthConfig;
import com.lucimber.dbus.connection.sasl.SaslExternalAuthConfig;
import com.lucimber.dbus.protocol.types.UInt32;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.unix.UnixChannel;
import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SocketChannelInitializer extends ChannelInitializer<UnixChannel> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final NettyConnection connection;
  private final String cookiePath;
  private final CompletableFuture<Void> initFuture;
  private final String userId;

  SocketChannelInitializer(final String userId, final String cookiePath, final NettyConnection connection,
                           final CompletableFuture<Void> initFuture) {
    this.userId = Objects.requireNonNull(userId);
    this.cookiePath = Objects.requireNonNull(cookiePath);
    this.connection = Objects.requireNonNull(connection);
    this.initFuture = Objects.requireNonNull(initFuture);
  }

  private LinkedHashMap<SaslAuthMechanism, SaslAuthConfig> buildAuthMechanismList() {
    LoggerUtils.debug(LOGGER, () -> "Building list of supported authentication mechanisms.");
    final LinkedHashMap<SaslAuthMechanism, SaslAuthConfig> map = new LinkedHashMap<>();
    map.put(SaslAuthMechanism.EXTERNAL, new SaslExternalAuthConfig(userId));
    map.put(SaslAuthMechanism.COOKIE, new SaslCookieAuthConfig(userId, cookiePath));
    map.put(SaslAuthMechanism.ANONYMOUS, new SaslAnonymousAuthConfig());
    return map;
  }

  @Override
  protected void initChannel(final UnixChannel ch) {
    LoggerUtils.debug(LOGGER, () -> "Initiating channel pipeline.");
    final ChannelPipeline pipeline = ch.pipeline();
    addSaslRelatedHandlers(pipeline);
    addDbusRelatedHandlers(pipeline);
    pipeline.addLast(connection.getClass().getSimpleName(), connection);
  }

  private void addDbusRelatedHandlers(final ChannelPipeline pipeline) {
    LoggerUtils.debug(LOGGER, () -> "Adding D-Bus related handlers to channel pipeline.");
    pipeline.addLast(FrameEncoder.class.getSimpleName(), new FrameEncoder());
    pipeline.addLast(OutboundMessageEncoder.class.getSimpleName(), new OutboundMessageEncoder());
    pipeline.addLast(ByteBufDecoder.class.getSimpleName(), new ByteBufDecoder());
    pipeline.addLast(FrameDecoder.class.getSimpleName(), new FrameDecoder());
    final UInt32 nameHandlerSerial = connection.getNextSerial();
    pipeline.addLast(MandatoryNameHandler.class.getSimpleName(), new MandatoryNameHandler(nameHandlerSerial));
    pipeline.addLast(ChannelInitCompleter.class.getSimpleName(), new ChannelInitCompleter(initFuture));
  }

  private void addSaslRelatedHandlers(final ChannelPipeline pipeline) {
    LoggerUtils.debug(LOGGER, () -> "Adding SASL related handlers to channel pipeline.");
    pipeline.addLast(SaslNulByteInboundHandler.class.getSimpleName(), new SaslNulByteInboundHandler());
    pipeline.addLast(SaslByteBufDecoder.class.getSimpleName(), new SaslByteBufDecoder());
    pipeline.addLast(SaslStringDecoder.class.getSimpleName(), new SaslStringDecoder());
    pipeline.addLast(SaslStringEncoder.class.getSimpleName(), new SaslStringEncoder());
    pipeline.addLast(SaslMessageEncoder.class.getSimpleName(), new SaslMessageEncoder());
    final LinkedHashMap<SaslAuthMechanism, SaslAuthConfig> authMechanisms = buildAuthMechanismList();
    pipeline.addLast(SaslMechanismInboundHandler.class.getSimpleName(),
            new SaslMechanismInboundHandler(authMechanisms));
    final SaslCompletionHandler saslCompletionHandler = new SaslCompletionHandler();
    pipeline.addLast(SaslCompletionHandler.class.getSimpleName(), saslCompletionHandler);
  }
}
