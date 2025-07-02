/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InternalTailHandler extends AbstractDuplexHandler implements InboundHandler, OutboundHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalTailHandler.class);

  @Override
  Logger getLogger() {
    return LOGGER;
  }
}
