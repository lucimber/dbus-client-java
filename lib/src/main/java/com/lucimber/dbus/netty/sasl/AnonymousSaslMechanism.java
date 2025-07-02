/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * Implements the ANONYMOUS SASL mechanism as defined in the D-Bus specification.
 * This mechanism requires no authentication or identity assertion and does not exchange payloads.
 */
public final class AnonymousSaslMechanism implements SaslMechanism {

  private static final String MECHANISM_NAME = "ANONYMOUS";

  private boolean complete = false;

  @Override
  public String getName() {
    return MECHANISM_NAME;
  }

  @Override
  public void init(ChannelHandlerContext ctx) {
    // No initialization or identity resolution needed.
  }

  @Override
  public Future<String> getInitialResponseAsync(ChannelHandlerContext ctx) {
    Promise<String> promise = ImmediateEventExecutor.INSTANCE.newPromise();
    // ANONYMOUS mechanism sends no payload — just "AUTH ANONYMOUS\r\n"
    complete = true;
    promise.setSuccess(null);
    return promise;
  }

  @Override
  public Future<String> processChallengeAsync(ChannelHandlerContext ctx, String challenge) {
    // According to spec, ANONYMOUS does not involve challenge/response.
    Promise<String> promise = ImmediateEventExecutor.INSTANCE.newPromise();
    promise.setFailure(new SaslMechanismException(
            "ANONYMOUS mechanism does not support server challenges. Received: " + challenge));
    return promise;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  @Override
  public void dispose() {
    // No state or sensitive data to clear.
    this.complete = false;
  }
}
