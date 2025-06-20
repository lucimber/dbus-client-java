/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

public class AnonymousSaslMechanism implements SaslMechanism {

  private boolean complete = false;

  @Override
  public String getName() {
    return "ANONYMOUS";
  }

  @Override
  public void init(ChannelHandlerContext ctx) {
    // No initialization needed for ANONYMOUS
  }

  @Override
  public Future<String> getInitialResponseAsync(ChannelHandlerContext ctx) {
    // ANONYMOUS has no initial response from client.
    // Client just sends "AUTH ANONYMOUS\r\n"
    // Server typically responds with "OK <guid>\r\n"
    complete = true; // Client part is done after sending AUTH ANONYMOUS
    Promise<String> promise = ImmediateEventExecutor.INSTANCE.newPromise();
    promise.setSuccess(null); // No initial response payload
    return promise;
  }

  @Override
  public Future<String> processChallengeAsync(ChannelHandlerContext ctx, String challenge) {
    // ANONYMOUS mechanism does not involve challenges.
    // If server sends one, it's a protocol violation or misunderstanding.
    Promise<String> promise = ImmediateEventExecutor.INSTANCE.newPromise();
    promise.setFailure(new SaslMechanismException("ANONYMOUS mechanism does not support challenges. Received: " + challenge));
    return promise;
  }

  @Override
  public boolean isComplete() {
    return complete;
  }

  @Override
  public void dispose() {
    // No sensitive data to dispose
  }
}
