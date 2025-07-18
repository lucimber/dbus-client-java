/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty;

import com.lucimber.dbus.util.LoggerUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.util.concurrent.Promise;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DBusChannelInitializer extends ChannelInitializer<Channel> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final RealityCheckpoint appLogicHandler;
  private final Promise<Void> connectPromise;

  DBusChannelInitializer(RealityCheckpoint appLogicHandler, Promise<Void> connectPromise) {
    this.appLogicHandler = Objects.requireNonNull(appLogicHandler);
    this.connectPromise = Objects.requireNonNull(connectPromise);
  }

  @Override
  protected void initChannel(Channel ch) {
    LOGGER.debug(LoggerUtils.CONNECTION, "Initiating channel.");

    ch.attr(DBusChannelAttribute.SERIAL_COUNTER).setIfAbsent(new AtomicLong(1));

    // Use centralized configuration for consistent handler ordering
    DBusHandlerConfiguration.initializePipeline(ch.pipeline(), connectPromise, appLogicHandler);
  }

}
