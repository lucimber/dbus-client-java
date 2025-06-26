/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.netty.sasl.SaslAuthenticationHandler;
import com.lucimber.dbus.netty.sasl.SaslCodec;
import com.lucimber.dbus.netty.sasl.SaslInitiationHandler;
import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

final class DBusChannelInitializer extends ChannelInitializer<Channel> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final AppLogicHandler appLogicHandler;
  private final Promise<Void> connectPromise;

  DBusChannelInitializer(AppLogicHandler appLogicHandler, Promise<Void> connectPromise) {
    this.appLogicHandler = Objects.requireNonNull(appLogicHandler);
    this.connectPromise = Objects.requireNonNull(connectPromise);
  }

  @Override
  protected void initChannel(Channel ch) {
    LoggerUtils.debug(LOGGER, () -> "Initiating channel.");
    ch.attr(DBusChannelAttribute.SERIAL_COUNTER).setIfAbsent(new AtomicLong(1));
    ChannelPipeline pipeline = ch.pipeline();
    addSaslRelatedHandlers(pipeline);
    addDbusRelatedHandlers(pipeline);
    pipeline.addLast(appLogicHandler);
  }

  private void addDbusRelatedHandlers(ChannelPipeline p) {
    LoggerUtils.debug(LOGGER, () -> "Adding D-Bus related handlers to channel pipeline.");
    p.addLast(FrameEncoder.class.getSimpleName(), new FrameEncoder());
    p.addLast(OutboundMessageEncoder.class.getSimpleName(), new OutboundMessageEncoder());
    p.addLast(FrameDecoder.class.getSimpleName(), new FrameDecoder());
    p.addLast(InboundMessageDecoder.class.getSimpleName(), new InboundMessageDecoder());
    p.addLast(DBusMandatoryNameHandler.class.getSimpleName(), new DBusMandatoryNameHandler());
    p.addLast(ConnectionCompletionHandler.class.getSimpleName(),
          new ConnectionCompletionHandler(connectPromise));
  }

  private void addSaslRelatedHandlers(ChannelPipeline p) {
    LoggerUtils.debug(LOGGER, () -> "Adding SASL related handlers to channel pipeline.");
    p.addLast(SaslInitiationHandler.class.getSimpleName(), new SaslInitiationHandler());
    p.addLast(SaslCodec.class.getSimpleName(), new SaslCodec());
    p.addLast(SaslAuthenticationHandler.class.getSimpleName(), new SaslAuthenticationHandler());
  }
}
