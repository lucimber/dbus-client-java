/*
 * Copyright 2023 Lucimber UG
 * Subject to the Apache License 2.0
 */

package com.lucimber.dbus.connection.impl;

import com.lucimber.dbus.connection.Connection;
import com.lucimber.dbus.connection.Handler;
import com.lucimber.dbus.connection.HandlerContext;
import com.lucimber.dbus.message.OutboundMessage;
import com.lucimber.dbus.util.LoggerUtils;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultHeadHandler implements Handler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Connection connection;

  DefaultHeadHandler(final Connection connection) {
    this.connection = Objects.requireNonNull(connection);
  }

  @Override
  public void onOutboundMessage(final HandlerContext ctx, final OutboundMessage msg) {
    LoggerUtils.debug(LOGGER, () -> "Passing an outbound message to the connection.");
    connection.sendMessage(msg);
  }
}
