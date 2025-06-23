/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.netty.sasl.SaslAuthenticationHandler;
import com.lucimber.dbus.netty.sasl.SaslCodec;
import com.lucimber.dbus.netty.sasl.SaslInitiationHandler;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.unix.UnixChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class SocketChannelInitializer extends ChannelInitializer<UnixChannel> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final NettyConnection connection;
  private final CompletableFuture<Void> initFuture;

  SocketChannelInitializer(NettyConnection connection, CompletableFuture<Void> initFuture) {
    this.connection = Objects.requireNonNull(connection);
    this.initFuture = Objects.requireNonNull(initFuture);
  }

  @Override
  protected void initChannel(UnixChannel ch) {
    LoggerUtils.debug(LOGGER, () -> "Initiating channel pipeline.");
    ChannelPipeline pipeline = ch.pipeline();
    addSaslRelatedHandlers(pipeline);
    addDbusRelatedHandlers(pipeline);
    pipeline.addLast(connection.getClass().getSimpleName(), connection);
  }

  private void addDbusRelatedHandlers(ChannelPipeline pipeline) {
    LoggerUtils.debug(LOGGER, () -> "Adding D-Bus related handlers to channel pipeline.");
    pipeline.addLast(FrameEncoder.class.getSimpleName(), new FrameEncoder());
    pipeline.addLast(OutboundMessageEncoder.class.getSimpleName(), new OutboundMessageEncoder());
    pipeline.addLast(FrameDecoder.class.getSimpleName(), new FrameDecoder());
    pipeline.addLast(InboundMessageDecoder.class.getSimpleName(), new InboundMessageDecoder());
    pipeline.addLast(DBusMandatoryNameHandler.class.getSimpleName(), new DBusMandatoryNameHandler());
    pipeline.addLast(ChannelInitCompleter.class.getSimpleName(), new ChannelInitCompleter(initFuture));
  }

  private void addSaslRelatedHandlers(ChannelPipeline pipeline) {
    LoggerUtils.debug(LOGGER, () -> "Adding SASL related handlers to channel pipeline.");
    pipeline.addLast(SaslInitiationHandler.class.getSimpleName(), new SaslInitiationHandler());
    pipeline.addLast(SaslCodec.class.getSimpleName(), new SaslCodec());
    pipeline.addLast(SaslAuthenticationHandler.class.getSimpleName(), new SaslAuthenticationHandler());
  }
}
