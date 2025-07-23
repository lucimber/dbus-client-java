/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lucimber.dbus.netty.sasl;

import java.nio.charset.StandardCharsets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExternalSaslMechanism implements SaslMechanism {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalSaslMechanism.class);

    private String authorizationId;
    private boolean complete = false;

    @Override
    public String getName() {
        return "EXTERNAL";
    }

    @Override
    public void init(ChannelHandlerContext ctx) throws SaslMechanismException {
        this.authorizationId = AuthorizationIdResolver.resolve();
        if (authorizationId == null || authorizationId.isEmpty()) {
            throw new SaslMechanismException("EXTERNAL authorization ID is missing.");
        }
        LOGGER.debug("EXTERNAL initialized with ID: {}", authorizationId);
    }

    @Override
    public Future<String> getInitialResponseAsync(ChannelHandlerContext ctx) {
        Promise<String> promise = ctx.executor().newPromise();
        findWorkerExecutor(ctx)
                .execute(
                        () -> {
                            if (authorizationId == null) {
                                promise.setFailure(
                                        new SaslMechanismException(
                                                "Authorization ID not initialized."));
                                return;
                            }
                            try {
                                String hexResponse =
                                        SaslUtil.hexEncode(
                                                authorizationId.getBytes(
                                                        StandardCharsets.US_ASCII));
                                promise.setSuccess(hexResponse);
                                this.complete = true;
                            } catch (Exception e) {
                                promise.setFailure(
                                        new SaslMechanismException(
                                                "Failed to hex-encode EXTERNAL ID", e));
                            }
                        });
        return promise;
    }

    @Override
    public Future<String> processChallengeAsync(ChannelHandlerContext ctx, String challenge) {
        Promise<String> promise = ctx.executor().newPromise();
        promise.setFailure(
                new SaslMechanismException(
                        "EXTERNAL does not support challenges. Got: " + challenge));
        return promise;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void dispose() {
        this.authorizationId = null;
        this.complete = false;
    }

    private EventExecutor findWorkerExecutor(ChannelHandlerContext ctx) {
        return GlobalEventExecutor.INSTANCE;
    }
}
